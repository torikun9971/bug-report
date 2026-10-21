import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.4.21"
}

group = ""
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    mingwX64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            executable {
                entryPoint = "main"
            }
        }

        compilations.configureEach {
            cinterops {
                create("openal")
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation("com.squareup.okio:okio:3.18.2")
        }
    }
}
