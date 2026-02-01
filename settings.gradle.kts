rootProject.name = "BandTrack"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mvn.tarsos.org/content/groups/public/") }
    }
}

include(":androidApp")
include(":shared")
