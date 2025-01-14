import it.krzeminski.readFirstClassFileFromJar
import it.krzeminski.readVersions
import java.io.FileOutputStream
import java.net.URI

fun main() {
    val groupId = "io.github.typesafegithub"
    val artifactId = "github-workflows-kt"
    val pathToMavenMetadata = "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/maven-metadata.xml"

    val mavenMetadata = URI(pathToMavenMetadata).toURL().readText()
    val versionNumberPattern = Regex("<version>(?<number>[^<]+)</version>")
    val versions = versionNumberPattern.findAll(mavenMetadata)
        .map { it.groups["number"]?.value }.toList()

    versions.forEach { version ->
        println("Version: $version")
        val pathToJar = "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
        val localJarFile = System.getProperty("java.io.tmpdir") + version + ".jar"
        URI(pathToJar).toURL().openStream().use { input ->
            val fos = FileOutputStream(localJarFile)
            input.copyTo(fos)
        }
        val firstClassFile: ByteArray = readFirstClassFileFromJar(localJarFile)
        val (bytecodeVersion, kotlinMetadataVersion) = readVersions(firstClassFile)
        println("  Bytecode version: $bytecodeVersion")
        println("  Kotlin metadata version: $kotlinMetadataVersion")
    }
}
