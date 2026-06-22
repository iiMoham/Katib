plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.katib.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.katib.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Base URL of the Katib proxy. Override per build type below.
        buildConfigField("String", "PROXY_BASE_URL", "\"http://10.0.2.2:8000\"")
        // Optional shared secret matching the proxy's PROXY_API_KEY (empty = none).
        buildConfigField("String", "PROXY_API_KEY", "\"\"")
    }

    buildTypes {
        debug {
            // 10.0.2.2 is the host machine as seen from the Android emulator.
            buildConfigField("String", "PROXY_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO: point this at your deployed proxy (https://...).
            buildConfigField("String", "PROXY_BASE_URL", "\"https://YOUR-PROXY.up.railway.app\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.billing.ktx)
}
