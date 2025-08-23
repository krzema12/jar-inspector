@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.2.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    wasmJs {
        binaries.executable()
        outputModuleName = "jarInspector"
        browser {
            commonWebpackConfig {
                outputFileName = "jarInspector.js"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.squareup.okio:okio:3.10.2")
                implementation("com.squareup.okio:okio-fakefilesystem:3.10.2")
                implementation("io.ktor:ktor-client-core:3.2.3")
                implementation("io.ktor:ktor-client-cio:3.2.3")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3.1")
                implementation(npm("pako", "2.1.0"))
                implementation(npm("jszip", "3.10.1"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-jszip:2025.7.14-3.10.1")
            }
        }
    }
}
