plugins {
    // Android
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    
    // Kotlin
    kotlin("android") version "1.9.22" apply false
    kotlin("multiplatform") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    
    // Firebase
    id("com.google.gms.google-services") version "4.4.1" apply false

    // KSP (Symbol Processing) pour Room
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
