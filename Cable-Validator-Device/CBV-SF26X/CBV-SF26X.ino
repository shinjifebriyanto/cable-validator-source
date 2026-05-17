#include <Arduino.h> 
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Preferences.h>
#include <Wire.h>
#include <Adafruit_ADS1X15.h>

// =========================================================================
// KONFIGURASI BLE (Bluetooth Low Energy)
// =========================================================================
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" 
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" 

// =========================================================================
// DEFINISI PIN
// =========================================================================
#define LED_BLE_PIN 8      
#define LED_CABLE_PIN 5    
#define BUZZER_PIN 4       

// =========================================================================
// KONFIGURASI PERANGKAT KERAS & PENGUKURAN
// =========================================================================
const int LED_BRIGHTNESS = 100;  
const int BUZZER_VOLUME  = 230;  

Adafruit_ADS1115 ads;            
const float CONSTANT_CURRENT_MA = 127.5; 
const float R_OFFSET = 0.00100;          
const int SAMPLES = 20;                  

// =========================================================================
// VARIABEL GLOBAL
// =========================================================================
BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;
bool deviceConnected = false;    
Preferences preferences;         

String currentConfigString = "CFG:SNI:0.75 mm²:1.0:0.02600"; 
float currentLimit = 0.02600;    

enum MeasureState { STATE_IDLE, STATE_SETTLING, STATE_HOLD };
MeasureState currentState = STATE_IDLE;
int settleCounter = 0;
const int SETTLE_MAX = 4;        
String lockedResult = "";        
bool isTestRequested = false;    

// =========================================================================
// CALLBACK SERVER BLUETOOTH
// =========================================================================
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println("[BLE] Smartphone Terhubung.");
        digitalWrite(LED_BLE_PIN, LOW); 
        
        // UMPAN BALIK AUDIO: Bluetooth Terhubung -> 2 Bip Pendek Cepat
        for(int i = 0; i < 2; i++) {
            analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
            delay(50); 
            analogWrite(BUZZER_PIN, 0); 
            delay(50);
        }
    };
    
    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        currentState = STATE_IDLE; 
        Serial.println("[BLE] Smartphone Terputus. Mereset memori...");
        analogWrite(LED_CABLE_PIN, 0); 
        
        pTxCharacteristic->setValue("IDLE:0.000");
        lockedResult = "";
        
        // UMPAN BALIK AUDIO: Bluetooth Terputus -> 3 Bip Panjang (Peringatan)
        for(int i = 0; i < 3; i++) {
            analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
            delay(200); 
            analogWrite(BUZZER_PIN, 0); 
            delay(100); 
        }
        
        delay(500); 
        pServer->startAdvertising(); 
        Serial.println("[SYSTEM] BLE Advertising Dinyalakan Kembali...");
    }
};

// =========================================================================
// CALLBACK PENERIMA DATA (RX)
// =========================================================================
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        String rxValue = pCharacteristic->getValue().c_str(); 
        
        if (rxValue.length() > 0) {
            if (rxValue.indexOf("START_TEST") >= 0) {
                isTestRequested = true;
                Serial.println("[BLE] Perintah Diterima dari Smartphone.");
            }
            else if (rxValue.indexOf("LIMIT:") >= 0) {
                String limitStr = rxValue.substring(6); 
                currentLimit = limitStr.toFloat();      
                
                preferences.begin("cable_conf", false); 
                preferences.putFloat("limit", currentLimit);
                preferences.end();
                
                Serial.println("[CONFIG] Batas Max Tersimpan di Perangkat: " + String(currentLimit, 5));
            }
        }
    }
};

// =========================================================================
// FUNGSI SETUP (INISIALISASI AWAL)
// =========================================================================
void setup() {
    Serial.begin(115200); 
    delay(100); 
    Serial.println("\n[SYSTEM] Memulai Inisialisasi Sistem...");
    
    pinMode(LED_BLE_PIN, OUTPUT); 
    pinMode(LED_CABLE_PIN, OUTPUT); 
    pinMode(BUZZER_PIN, OUTPUT);
    
    digitalWrite(LED_BLE_PIN, HIGH); 
    analogWrite(LED_CABLE_PIN, 0); 
    analogWrite(BUZZER_PIN, 0);        

    Wire.begin(6, 7); 
    ads.begin(); 
    ads.setGain(GAIN_ONE); 

    preferences.begin("cable_conf", true);
    currentConfigString = preferences.getString("config", "CFG:SNI:0.75 mm²:1.0:0.02600"); 
    preferences.end();
    
    int lastColon = currentConfigString.lastIndexOf(':');
    
    if (lastColon > 0) {
        currentLimit = currentConfigString.substring(lastColon + 1).toFloat();
    }
    
    BLEDevice::init("CBV-001X"); 
    pServer = BLEDevice::createServer(); 
    pServer->setCallbacks(new MyServerCallbacks());
    
    BLEService *pService = pServer->createService(SERVICE_UUID);
    BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
    pRxCharacteristic->setCallbacks(new MyCallbacks());
    
    pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
    pTxCharacteristic->addDescriptor(new BLE2902());
    
    pService->start();
    
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID); 
    pAdvertising->setScanResponse(false); 
    pAdvertising->setMinPreferred(0x0);  
    BLEDevice::startAdvertising();
    
    // UMPAN BALIK AUDIO: Sistem Siap Digunakan -> 1 Bip Agak Panjang
    analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
    delay(300); 
    analogWrite(BUZZER_PIN, 0);
    
    Serial.println("[SYSTEM] Sistem Siap. Perangkat menanti instruksi...");
}

// =========================================================================
// FUNGSI LOOP (SIKLUS UTAMA)
// =========================================================================
void loop() {
    // Memeriksa koneksi perangkat
    if (!deviceConnected) {
        digitalWrite(LED_BLE_PIN, !digitalRead(LED_BLE_PIN)); 
        analogWrite(LED_CABLE_PIN, 0); 
        currentState = STATE_IDLE; 
        delay(500); 
        return; 
    }

    // Mengambil sampel data pembacaan ADC
    long total_raw = 0; 
    for(int i = 0; i < SAMPLES; i++) {
        total_raw += ads.readADC_Differential_0_1(); 
        delay(5); 
    }
    
    float avg_raw = (float)total_raw / SAMPLES; 

    // Deteksi apakah kabel terpasang
    bool isCableAttached = (avg_raw < 28000 && avg_raw > -28000);

    // --- 1. PROSES JIKA TOMBOL VALIDASI DITEKAN DI SMARTPHONE ---
    if (isTestRequested) {
        Serial.println("[STATUS] Mengeksekusi permintaan Validasi...");
        
        if (!isCableAttached) {
            Serial.println("[ERROR] Kabel/Probe belum dipasang. Proses dibatalkan.");
            pTxCharacteristic->setValue("ERROR:0.000"); 
            pTxCharacteristic->notify();
            
            isTestRequested = false; 
            currentState = STATE_IDLE;
            
            // UMPAN BALIK AUDIO: Error -> 3 Bip Cepat
            for(int i = 0; i < 3; i++) {
                analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
                delay(100);
                analogWrite(BUZZER_PIN, 0); 
                delay(50);
            }
        } else {
            Serial.println("[STATUS] Kabel terdeteksi. Menstabilkan (2 detik)...");
            currentState = STATE_SETTLING; 
            settleCounter = 0; 
            isTestRequested = false; 
            
            analogWrite(LED_CABLE_PIN, LED_BRIGHTNESS); 
            
            // UMPAN BALIK AUDIO: Tombol Ditekan / Mulai Proses -> 1 Bip Sangat Pendek
            analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
            delay(50); 
            analogWrite(BUZZER_PIN, 0);
        }
    }

    // --- 2. ALUR PENGUKURAN 2 DETIK (FASE SETTLING) ---
    if (currentState == STATE_SETTLING) {
        if (!isCableAttached) {
            currentState = STATE_IDLE;
            Serial.println("[ERROR] Kabel terlepas saat validasi! Proses dibatalkan.");
            
            pTxCharacteristic->setValue("ERROR:0.000"); 
            pTxCharacteristic->notify();
            analogWrite(LED_CABLE_PIN, 0); 
            
            // UMPAN BALIK AUDIO: Error Kabel Terlepas -> 3 Bip Cepat
            for(int i = 0; i < 3; i++) {
                analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
                delay(100);
                analogWrite(BUZZER_PIN, 0); 
                delay(50);
            }
        } else {
            // Menghitung tegangan dan resistansi
            float voltage_mV = avg_raw * 0.125F; 
            
            if (voltage_mV < 0) {
                voltage_mV = voltage_mV * -1.0; 
            }
            
            float resistance_Ohm = (voltage_mV / CONSTANT_CURRENT_MA) - R_OFFSET;
            
            if (resistance_Ohm < 0) {
                resistance_Ohm = 0.00000; 
            }

            settleCounter++; 
            
            String tempMsg = "MENGUKUR:" + String(resistance_Ohm, 3);
            pTxCharacteristic->setValue(tempMsg.c_str()); 
            pTxCharacteristic->notify();

            // Kunci Hasil jika sudah mencapai batas siklus maksimal (~ 2 detik)
            if (settleCounter >= SETTLE_MAX) {
                currentState = STATE_HOLD;
                
                if (resistance_Ohm <= currentLimit) {
                    lockedResult = "LULUS:" + String(resistance_Ohm, 5);
                    
                    // UMPAN BALIK AUDIO: LULUS (PASS) -> 2 Bip Sedang
                    for(int i = 0; i < 2; i++) {
                        analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
                        delay(150);
                        analogWrite(BUZZER_PIN, 0); 
                        delay(100);
                    }
                } else {
                    lockedResult = "GAGAL:" + String(resistance_Ohm, 5);
                    
                    // UMPAN BALIK AUDIO: GAGAL (FAIL) -> 3 Bip Cepat (Peringatan)
                    for(int i = 0; i < 3; i++) {
                        analogWrite(BUZZER_PIN, BUZZER_VOLUME); 
                        delay(100);
                        analogWrite(BUZZER_PIN, 0); 
                        delay(50);
                    }
                }
                
                Serial.println("[RESULT] Validasi Selesai -> " + lockedResult);
                pTxCharacteristic->setValue(lockedResult.c_str()); 
                pTxCharacteristic->notify();
            }
        }
    } 
    // --- 3. KONDISI JIKA SISTEM SEDANG IDLE ATAU HOLD ---
    else {
        if (!isCableAttached) {
            analogWrite(LED_CABLE_PIN, 0); 
        }
    }
    
    // Jeda sebelum siklus berikutnya
    delay(500); 
}
