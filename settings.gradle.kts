import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
include(":data:problems-api")
include(":data:problems-impl")
include(":domain:problems-api")
include(":domain:problems-impl")
include(":feature:problemfinder-api")
include(":feature:problemfinder-impl")
