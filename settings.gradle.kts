plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "jar-inspector"

include(":app")
include(":library")
includeBuild("test-module-to-inspect")
