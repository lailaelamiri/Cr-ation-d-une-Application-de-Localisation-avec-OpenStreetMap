// ─────────────────────────────────────────────
// Build configuration for MapApplication
// Uses Kotlin DSL for stronger type-checking
// ─────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
}

android {
    // App identity
    namespace = "com.example.mapapplication"

    // We compile against SDK 34 so we can use the latest Android APIs
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mapapplication"

        // Floor at API 24 (Android 7.0) — covers ~95% of active devices
        minSdk = 24

        // Target the current stable SDK for best-practice behavior
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        // Standard test runner for instrumented tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Minification is off — keeps debugging straightforward during learning
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 8 unlocks lambdas, streams, and other modern syntax
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // ── Core AndroidX ──────────────────────────────────────────────────────────
    implementation(libs.appcompat)        // Backward-compat Activity & Fragment APIs
    implementation(libs.material)         // Google Material Design components
    implementation(libs.activity)         // ActivityResultContracts & modern lifecycle
    implementation(libs.constraintlayout) // Powerful flat-hierarchy layouts

    // ── Networking ─────────────────────────────────────────────────────────────
    implementation(libs.volley)           // Handles HTTP queuing, caching & retries

    // ── Mapping ────────────────────────────────────────────────────────────────
    implementation(libs.maps.core)        // Shared map utilities (GeoPoint, BoundingBox…)
    implementation(libs.osmdroid)         // OpenStreetMap tile rendering for Android

    // ── Testing ────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}