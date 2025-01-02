plugins {
    kotlin("jvm") version "2.1.0"
}

group = "it.krzeminski"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
    implementation("io.exoquery:pprint-kotlin:2.0.2")
}
