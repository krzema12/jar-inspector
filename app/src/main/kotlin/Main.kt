import it.krzeminski.readFirstClassFileFromJar
import it.krzeminski.readVersions

fun main() {
    val firstClassFile: ByteArray = readFirstClassFileFromJar(
        "test-module-to-inspect/build/libs/test-module-to-inspect.jar")
    val (bytecodeVersion, kotlinMetadataVersion) = readVersions(firstClassFile)
    println("Bytecode version: $bytecodeVersion")
    println("Kotlin metadata version: $kotlinMetadataVersion")
}

