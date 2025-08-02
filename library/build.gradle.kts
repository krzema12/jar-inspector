plugins {
    kotlin("multiplatform") version "2.1.0"
}

group = "it.krzeminski"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api("com.squareup.okio:okio:3.10.2")
                implementation("io.exoquery:pprint-kotlin:2.0.2")
            }
        }
    }
}
