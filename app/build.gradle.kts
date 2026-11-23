plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.rippleeffect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.rippleeffect"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val dataConnectServiceUrl = (project.findProperty("DATACONNECT_SERVICE_URL") as String?) ?: ""
        val dataConnectApiKey = (project.findProperty("DATACONNECT_API_KEY") as String?) ?: ""
        val useDataConnectEmulator = ((project.findProperty("DATACONNECT_USE_EMULATOR") as String?) ?: "false").toBoolean()

        buildConfigField("String", "DATACONNECT_SERVICE_URL", "\"$dataConnectServiceUrl\"")
        buildConfigField("String", "DATACONNECT_API_KEY", "\"$dataConnectApiKey\"")
        buildConfigField("boolean", "DATACONNECT_USE_EMULATOR", useDataConnectEmulator.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Ship only the app's own languages. Libraries bring translations for many
    // more (e.g. French); if those stay in the APK, Android treats them as
    // supported, so a phone set to French-then-Spanish would get English
    // app text instead of Spanish. Keep in step with res/xml/locales_config.xml.
    androidResources {
        localeFilters += listOf("en", "es", "sw", "pt")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-dataconnect")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation ("org.mongodb:mongodb-driver-sync:4.2.3")
    // Add extended icons dependency
    implementation("androidx.compose.material:material-icons-extended")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
