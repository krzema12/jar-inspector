import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import it.krzeminski.readFirstClassFileFromJar
import it.krzeminski.readVersions
import java.io.File

suspend fun main() {
    val groupId = "io.github.typesafegithub"
    val artifactId = "github-workflows-kt"
    val pathToMavenMetadata = "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/maven-metadata.xml"

    val httpClient = HttpClient()

    val mavenMetadata = httpClient.get(urlString = pathToMavenMetadata) {}.bodyAsText()
    val versionNumberPattern = Regex("<version>(?<number>[^<]+)</version>")
    val versions = versionNumberPattern.findAll(mavenMetadata)
        .map { it.groups["number"]?.value }.toList()

    versions.forEach { version ->
        println("Version: $version")
        val pathToJar = "https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
        val localJarFile = System.getProperty("java.io.tmpdir") + version + ".jar"
        val jarResponse = httpClient.get(urlString = pathToJar) {}.body<ByteArray>()
        File(localJarFile).writeBytes(jarResponse)
        val firstClassFile: ByteArray = readFirstClassFileFromJar(localJarFile)
        val (bytecodeVersion, kotlinMetadataVersion) = readVersions(firstClassFile)
        println("  Bytecode version: $bytecodeVersion")
    }
}
