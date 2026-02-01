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
        maven { url = uri("https://mvn.0110.be/releases") }
    }
}

include(":androidApp")
include(":shared")
