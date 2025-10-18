plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}


android {
    namespace = "com.example.capstone"
    compileSdk = 36



    defaultConfig {
        applicationId = "com.example.capstone"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//Namra acc reach quota
 //       buildConfigField ("String", "GOOGLE_API_KEY", "\"AIzaSyBJMYBvYDLD3Rwqjmu3cjlqJCmVL8KBWyk\"")
        //Armand Acc
        buildConfigField ("String", "GOOGLE_API_KEY", "\"AIzaSyAdwPGAfSjx37JAYY9LLE-dFoOfAmqc_b8\"")

        buildConfigField ("String", "GOOGLE_SEARCH_CX", "\"43166e3c70bf140ce\"")


        // buildConfigField("String", "PIXABAY_API_KEY", "\"52755156-fe830c505bd981aa4a8b221a2\"")


    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.navigation.compose)

    implementation("androidx.compose.material:material-icons-extended:1.5.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil:2.5.0")
    //implementation("androidx.compose.material:material:1.4.0")

    implementation("androidx.compose.material:material:1.5.4")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.json:json:20230227")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.material3)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    val cameraxVersion = "1.2.3"
//CameraX
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")
// Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    // ML Kit Pose Detection
    implementation("com.google.mlkit:pose-detection:18.0.0-beta5")
        // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx") // optional (for images)
    implementation("com.google.android.gms:play-services-auth:20.4.1")

        // your other dependencies (compose, cameraX, etc) stay the same
// Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.5.0")

    // For file handling
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
// ML Kit - Subject Segmentation (Background Removal)
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta5")  // Keep this for auto-detect
    implementation("com.google.mlkit:vision-common:17.3.0")

    // Optional but recommended for better image processing
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
    implementation ("androidx.navigation:navigation-compose:2.5.3")


}