import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

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
        val jarResponse = httpClient.get(urlString = pathToJar) {}.body<ByteArray>()
        val firstClassFile: ByteArray = readFirstClassFileFromJar(jarResponse)
        val (bytecodeVersion, kotlinMetadataVersion) = readVersions(firstClassFile)
        println("  Bytecode version: $bytecodeVersion")
    }
}
