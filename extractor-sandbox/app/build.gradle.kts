plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.axiomlabs.vidamacraft.sandbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.axiomlabs.vidamacraft.sandbox"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-sandbox"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
}
