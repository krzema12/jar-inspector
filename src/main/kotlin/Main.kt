package it.krzeminski

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.readUByte
import kotlinx.io.readUInt
import kotlinx.io.readUShort

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Reading a class file")
    val classFileToRead = Path("test-module-to-inspect/build/classes/kotlin/main/SomeClass.class")
    val source = SystemFileSystem.source(classFileToRead).buffered()

    // https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
    val magic = source.readUInt()
    println("Magic: ${magic.toHexString()}")

    val minorVersion = source.readUShort()
    val majorVersion = source.readUShort()
    println("Version: $majorVersion.$minorVersion")

    val constantPoolCount = source.readUShort()
    println("Constant pool count: $constantPoolCount")

    repeat(constantPoolCount.toInt() - 1) { itemIndex ->
        println("  Constant pool item $itemIndex")
        readConstantPoolEntry(source)
    }
}

private fun readConstantPoolEntry(source: Source) {
    val tag = source.readUByte().toInt()
    print("    Tag: $tag - ")

    when (tag) {
        1 -> readUtf8Info(source)
        3 -> readInteger(source)
        7 -> readClass(source)
        8 -> readString(source)
        10 -> readMethodref(source)
        11 -> readInterfaceMethodref(source)
        12 -> readNameAndType(source)
        else -> error("Unexpected tag $tag")
    }
}

private fun readUtf8Info(source: Source) {
    println("UTF-8")
    val length = source.readUShort()
    val string = source.readString(length.toLong())
    println("    String: $string")
}

private fun readInteger(source: Source) {
    println("Integer")
    val value = source.readInt()
    println("    Value: $value")
}

private fun readClass(source: Source) {
    println("Class")
    val nameIndex = source.readUShort().toInt()
    println("    Name index: $nameIndex")
}

private fun readString(source: Source) {
    println("String")
    val stringIndex = source.readUShort().toInt()
    println("    String index: $stringIndex")
}

private fun readMethodref(source: Source) {
    println("Method ref")
    val classIndex = source.readUShort().toInt()
    val nameAndTypeIndex = source.readUShort().toInt()
    println("    Class index: $classIndex, name and type index: $nameAndTypeIndex")
}

private fun readInterfaceMethodref(source: Source) {
    println("Interface method ref")
    val classIndex = source.readUShort().toInt()
    val nameAndTypeIndex = source.readUShort().toInt()
    println("    Class index: $classIndex, name and type index: $nameAndTypeIndex")
}

private fun readNameAndType(source: Source) {
    println("Name and type")
    val nameIndex = source.readUShort().toInt()
    val descriptorIndex = source.readUShort().toInt()
    println("    Name index: $nameIndex, descriptor index: $descriptorIndex")
}
