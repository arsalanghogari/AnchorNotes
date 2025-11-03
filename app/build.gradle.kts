plugins {
    alias(libs.plugins.android.application)
}

// +++ 1. IMPORT THE PROPERTIES CLASS (KOTLIN SYNTAX) +++
import java.util.Properties

// +++ 2. READ THE LOCAL.PROPERTIES FILE (KOTLIN SYNTAX) +++
val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "edu.usc.cs310.anchornotes"
    compileSdk = 36

    // +++ 3. ENABLE BUILDCONFIG FEATURE (KOTLIN SYNTAX) +++
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "edu.usc.cs310.anchornotes"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // +++ 4. MAKE KEY AVAILABLE TO THE MANIFEST (KOTLIN SYNTAX) +++
        manifestPlaceholders["mapsApiKey"] = properties.getProperty("MAPS_API_KEY", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // +++ 5. MAKE KEY AVAILABLE TO JAVA CODE (KOTLIN SYNTAX) +++
            buildConfigField("String", "MAPS_API_KEY", "\"${properties.getProperty("MAPS_API_KEY")}\"")
        }
        debug {
            // Also add for debug builds
            buildConfigField("String", "MAPS_API_KEY", "\"${properties.getProperty("MAPS_API_KEY")}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.5")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
}