plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.problemfinder.feature.projects.impl"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":domain:workspaces:workspaces-api"))
    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(project(":domain:workspaces:workspaces-api")))
    androidTestImplementation(testFixtures(project(":domain:workspaces:workspaces-api")))
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.ui.test.manifest)
    api(project(":feature:projects:projects-api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:mvi"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.hilt.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    debugImplementation(libs.androidx.ui.tooling)

    ksp(libs.hilt.compiler)
}
