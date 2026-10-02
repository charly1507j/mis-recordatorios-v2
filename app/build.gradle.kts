plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.misrecordatorios.v2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.misrecordatorios.v2"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
