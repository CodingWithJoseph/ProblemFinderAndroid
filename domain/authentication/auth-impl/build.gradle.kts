plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.problemfinder.domain.authentication.impl"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":domain:authentication:auth-api"))
    implementation(project(":data:authentication:auth-api"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
