import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip

fun readFirstClassFileFromJar(fileSystem: FileSystem): ByteArray {
    val zipFileSystem = fileSystem.openZip("some-jar.jar".toPath())
    val classFilePath = zipFileSystem.listRecursively(".".toPath()).first { it.name.endsWith(".class") }
    println("  Analyzed class file path: $classFilePath")
    return zipFileSystem.source(classFilePath).buffer().readByteArray()
}

fun readVersions(byteArray: ByteArray): Pair<String, String> {
    val source = Buffer().apply { write(byteArray) }
    val classFile = readClassFile(source = source)
    val bytecodeVersion = "${classFile.majorVersion}.${classFile.minorVersion}"
    return Pair(bytecodeVersion, "unknown")
}


fun readClassFile(source: BufferedSource): ClassFile {
    // https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
    val magic = source.readInt()
    require(magic.toUInt() == 0xCAFEBABE.toUInt()) { "Magic is invalid: $magic" }

    val minorVersion = source.readShort()
    val majorVersion = source.readShort()

    return ClassFile(
        majorVersion = majorVersion,
        minorVersion = minorVersion,
    )
}
