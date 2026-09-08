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

rootProject.name = "BluePlayer"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:player")
include(":ui:theme")
include(":ui:components")
include(":feature:library")
include(":feature:albums")
include(":feature:nowplaying")
include(":feature:artists")
include(":feature:folders")
include(":feature:settings")
include(":core:database")
include(":feature:favorites")
include(":feature:playlists")
include(":feature:search")
include(":feature:genres")
include(":feature:queue")