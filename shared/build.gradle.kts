plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") // Plugin KSP pour Room
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Kotlinx Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                // AndroidX Core
                implementation("androidx.core:core-ktx:1.12.0")

                // Lifecycle
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

                // Room Database
                implementation("androidx.room:room-runtime:2.6.1")
                implementation("androidx.room:room-ktx:2.6.1")

                // Firebase (utilise le BoM pour la gestion des versions)
                implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
                implementation("com.google.firebase:firebase-auth-ktx")
                implementation("com.google.firebase:firebase-firestore-ktx")
                implementation("com.google.firebase:firebase-messaging-ktx")

                // WorkManager
                implementation("androidx.work:work-runtime-ktx:2.9.0")

                // TarsosDSP (Audio Analysis)
                // TarsosDSP (Audio Analysis)
                implementation("be.tarsos.dsp:core:2.5")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

android {
    namespace = "com.bandtrack.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // KSP pour Android (Room)
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")
}
