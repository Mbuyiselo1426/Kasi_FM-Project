plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kasiefm"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.kasiefm"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

// Exclude transitive androidx.media pulls from ExoPlayer to avoid
// resource conflicts with the bundled media2/media3 copies.
configurations.all {
    exclude(group = "androidx.media", module = "media")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.exoplayer:exoplayer:2.18.7")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.fragment:fragment:1.6.2")

    // Retrofit + Gson for talking to the Kasie FM API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Lightweight, dependency-free IdlingResource type - lets MainActivity expose
    // when RadioService binding is in flight so Espresso can wait for it instead
    // of guessing with a sleep. Does not pull in the Espresso test runner itself.
    implementation("androidx.test.espresso:espresso-idling-resource:3.5.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
