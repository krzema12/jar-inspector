import it.krzeminski.readFirstClassFileFromJar
import it.krzeminski.readVersions

fun main() {
    val firstClassFile: ByteArray = readFirstClassFileFromJar()
    val (bytecodeVersion, kotlinMetadataVersion) = readVersions(firstClassFile)
    println("Bytecode version: $bytecodeVersion")
    println("Kotlin metadata version: $kotlinMetadataVersion")
}

