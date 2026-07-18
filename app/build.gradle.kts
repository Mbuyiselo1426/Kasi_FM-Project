plugins {
    id("com.android.application")  id("com.android.application") version "7.4.2" apply false
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
    implementation("com.google.android.material:material:1.9.0")  // ← add this

    // Retrofit + Gson for talking to the Kasie FM API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation("junit:junit:4.13.2")

}
