plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.leemon4ik.lemonxr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.leemon4ik.lemonxr"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2-beta"
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
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}
