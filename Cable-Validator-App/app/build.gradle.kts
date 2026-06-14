plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // <--- WAJIB DITAMBAHKAN UNTUK ROOM
}

android {
    namespace = "com.shinji.cablevalidator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shinji.cablevalidator"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Library Bluetooth Low Energy (FastBle)
    implementation("com.github.Jasonchenlijian:FastBle:2.4.0")

    // Opsional: Agar tampilan UI lebih bagus (Material Design update)
    // Note: Versi 1.9.0 sudah cukup stabil, tapi kalau ada conflict pakai libs.material saja
    implementation("com.google.android.material:material:1.9.0")

    // === ROOM DATABASE (DATABASE LOKAL) ===
    // Perbaikan sintaks: Pakai kurung () karena ini Kotlin DSL
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1") // <--- Butuh plugin 'kotlin-kapt' di atas

    // WAJIB DITAMBAHKAN UNTUK MEMPERBAIKI ERROR viewModelScope
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
}