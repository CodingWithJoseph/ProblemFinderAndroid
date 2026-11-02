plugins { alias(libs.plugins.kotlin.jvm); `java-test-fixtures` }
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":core:model"))
    api(libs.coroutines.core)
}
