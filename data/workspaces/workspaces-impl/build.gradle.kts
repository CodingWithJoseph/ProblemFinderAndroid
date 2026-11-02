plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
}
android {
    namespace = "com.example.problemfinder.data.workspaces.impl"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    api(project(":data:workspaces:workspaces-api"))
    implementation(project(":data:authentication:auth-api"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    testImplementation(libs.junit4)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
room3 { schemaDirectory("$projectDir/schemas") }
