plugins {
    kotlin("multiplatform") version "2.2.10"
    id("io.kotest") version "6.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        val test by testRuns.existing {
            executionTask {
                useJUnitPlatform()
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.squareup.okio:okio:3.10.2")
            }
        }

        commonTest {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:6.0.0")
                implementation("io.kotest:kotest-assertions-core:6.0.0")
            }
        }

        jvmTest {
            dependencies {
                runtimeOnly("org.junit.platform:junit-platform-launcher")
                runtimeOnly("io.kotest:kotest-runner-junit5:6.0.0")
            }
        }
    }
}
