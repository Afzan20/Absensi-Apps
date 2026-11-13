
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.absensiapps"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.absensiapps"

        // Supabase minimum Android API
        minSdk = 26

        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Existing dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

// Google ML Kit Face Detection
    implementation(libs.mlkit.face.detection)

    // Google LiteRT - TensorFlow Lite Interpreter API
    implementation("com.google.ai.edge.litert:litert:1.4.2")

    // Supabase BOM
    implementation(
        platform(
            "io.github.jan-tennert.supabase:bom:3.2.6"
        )
    )

    // Supabase Authentication
    implementation(
        "io.github.jan-tennert.supabase:auth-kt"
    )

    // Supabase Database
    implementation(
        "io.github.jan-tennert.supabase:postgrest-kt"
    )

    // Ktor Android Client
    implementation(
        "io.ktor:ktor-client-android:3.3.1"
    )

    // Unit Testing
    testImplementation(libs.junit)

    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
