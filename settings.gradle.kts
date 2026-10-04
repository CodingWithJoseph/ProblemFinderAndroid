import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProblemFinder"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:mvi")

include(":data:authentication:auth-api")
include(":data:authentication:auth-impl")

include(":domain:authentication:auth-api")
include(":domain:authentication:auth-impl")

include(":feature:authentication:auth-api")
include(":feature:authentication:auth-impl")

include(":feature:problems:problems-api")
include(":feature:problems:problems-impl")
