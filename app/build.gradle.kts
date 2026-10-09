plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.careerlens.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.careerlens.ai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val geminiKey = "AQ.Ab8RN6JFam8CoH7i9vL_OpiPNlgYnLlX51sEyIeb6dyZP4pMKQ"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
