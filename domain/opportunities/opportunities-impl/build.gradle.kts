plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.problemfinder.domain.opportunities.impl"
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
    api(project(":domain:opportunities:opportunities-api"))
    implementation(project(":data:opportunities:opportunities-api"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
}
