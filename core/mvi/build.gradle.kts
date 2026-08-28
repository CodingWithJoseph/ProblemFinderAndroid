plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.problemfinder.core.mvi"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.coroutines.core)
    testImplementation(libs.junit4)
}

