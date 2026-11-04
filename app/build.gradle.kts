import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.room) apply false
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
}

// Local developer configuration takes precedence over environment values (for example in CI).
// These values enter BuildConfig and are bundled in the app; use the publishable Supabase key.
fun setting(name: String): String =
    localProperties.getProperty(name)
        ?: System.getenv(name)
        ?: error("Missing $name")

android {
    namespace = "com.example.problemfinder.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.codingwithjoseph.immensity"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SUPABASE_URL", "\"${setting("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${setting("SUPABASE_PUBLISHABLE_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

// The app is the composition root: including implementation modules makes their Hilt bindings
// available while feature code can continue depending on API contracts.
dependencies {
    implementation(project(":core:preferences"))
    implementation(project(":data:workspaces:workspaces-impl"))
    implementation(project(":domain:workspaces:workspaces-impl"))
    implementation(project(":feature:details:details-impl"))
    implementation(project(":core:mvi"))
    implementation(project(":core:designsystem"))

    implementation(project(":data:authentication:auth-impl"))
    implementation(project(":domain:authentication:auth-impl"))
    implementation(project(":domain:authentication:auth-api"))
    implementation(project(":feature:authentication:auth-impl"))
    implementation(project(":feature:authentication:auth-api"))

    implementation(project(":feature:opportunities:opportunities-api"))
    implementation(project(":feature:opportunities:opportunities-impl"))
    implementation(project(":domain:opportunities:opportunities-impl"))
    implementation(project(":domain:opportunities:opportunities-api"))
    implementation(project(":data:opportunities:opportunities-impl"))
    implementation(project(":data:opportunities:opportunities-api"))

    implementation(project(":feature:builds:builds-api"))
    implementation(project(":feature:builds:builds-impl"))

    implementation(project(":feature:projects:projects-api"))
    implementation(project(":feature:projects:projects-impl"))

    implementation(project(":feature:search:search-api"))
    implementation(project(":feature:search:search-impl"))

    implementation(project(":feature:settings:settings-impl"))
    implementation(libs.hilt.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    ksp(libs.hilt.compiler)
    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.core)
}

