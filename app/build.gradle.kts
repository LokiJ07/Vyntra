plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.lokij07.vyntra"
    compileSdk = 35

    // Keep signing outside Git. Local debug builds do not need these variables.
    val stableStorePath = System.getenv("VYNTRA_SIGNING_STORE_FILE")
    val stableStorePassword = System.getenv("VYNTRA_SIGNING_PASSWORD")
    signingConfigs {
        create("vyntraRelease") {
            if (!stableStorePath.isNullOrBlank() && !stableStorePassword.isNullOrBlank()) {
                storeFile = file(stableStorePath)
                storePassword = stableStorePassword
                keyAlias = "vyntra"
                keyPassword = stableStorePassword
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.lokij07.vyntra"
        minSdk = 29
        targetSdk = 35
        // Each Actions run increments the code, enabling install-over-existing updates.
        // Never reuse the same versionCode for a new public release.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 10000 + it } ?: 8
        versionName = "0.2.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("vyntraRelease")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Free/MIT Android yt-dlp engine: downloads supported audio from permitted sources.
    // Native audio is retained (M4A/WebM/Opus/etc); MP3 transcoding is not bundled.
    implementation("dev.ffmpegkit-maintained:yt-dlp-android:2.0.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
