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

include(":feature:browse:browse-api")
include(":feature:browse:browse-impl")

include(":feature:search:search-api")
include(":feature:search:search-impl")

include(":feature:builds:builds-api")
include(":feature:builds:builds-impl")

include(":feature:projects:projects-api")
include(":feature:projects:projects-impl")

include(":feature:settings:settings-api")
include(":feature:settings:settings-impl")
