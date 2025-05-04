pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Or FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Required for GitHub dependencies
    }
}

rootProject.name = "LeHydroSys"
include(":app")
