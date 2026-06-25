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

rootProject.name = "IptvBoxPlayer"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:security")
include(":core:designsystem")
include(":core:player")
include(":data:playlist")
include(":domain")
