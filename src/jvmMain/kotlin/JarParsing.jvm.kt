import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.openZip

actual fun readFirstClassFileFromJar(byteArray: ByteArray): ByteArray {
    val fileSystem = FakeFileSystem()
    fileSystem.apply {
        write("/some-jar.jar".toPath()) {
            write(byteArray)
        }
    }

    val zipFileSystem = fileSystem.openZip("some-jar.jar".toPath())
    val classFilePath = zipFileSystem.listRecursively(".".toPath()).first { it.name.endsWith(".class") }
    println("  Analyzed class file path: $classFilePath")
    return zipFileSystem.source(classFilePath).buffer().readByteArray()
}
