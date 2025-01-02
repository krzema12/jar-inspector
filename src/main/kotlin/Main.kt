package it.krzeminski

import kotlinx.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

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

    val accessFlags = source.readUShort()
    println("Access flags: $accessFlags")

    val thisClass = source.readUShort()
    println("This class: $thisClass")

    val superclass = source.readUShort()
    println("Super class: $superclass")

    val interfacesCount = source.readUShort()
    println("Interfaces count: $interfacesCount")

    repeat(interfacesCount.toInt()) { itemIndex ->
        println("  Interface $itemIndex")
        val interfaceRef = source.readUShort()
        println("    Interface ref: $interfaceRef")
    }

    val fieldsCount = source.readUShort()
    println("Fields count: $fieldsCount")

    repeat(fieldsCount.toInt()) { itemIndex ->
        println("  Field $itemIndex")
        readFieldInfo(source)
    }

    val methodsCount = source.readUShort()
    println("Methods count: $methodsCount")

    repeat(methodsCount.toInt()) { itemIndex ->
        println("  Method $itemIndex")
        readMethodInfo(source)
    }

    val attributesCount = source.readUShort()
    println("Attributes count: $attributesCount")

    repeat(attributesCount.toInt()) { itemIndex ->
        println("  Attribute $itemIndex")
        readAttributeInfo(source)
    }

    require(source.exhausted()) {
        "There's still some data to read!"
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
        9 -> readFieldref(source)
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

private fun readFieldref(source: Source) {
    println("Fieldref")
    val classIndex = source.readUShort()
    println("    Class index: $classIndex")
    val nameAndTypeIndex = source.readUShort()
    println("    NameAndType index: $nameAndTypeIndex")
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

private fun readFieldInfo(source: Source) {
    val accessFlags = source.readUShort()
    println("    Access flags: $accessFlags")
    val nameIndex = source.readUShort()
    println("    Name index: $nameIndex")
    val descriptorIndex = source.readUShort()
    println("    Descriptor index: $descriptorIndex")
    val attributesCount = source.readUShort()
    println("    Attributes count: $attributesCount")

    repeat(attributesCount.toInt()) { itemIndex ->
        println("    Attribute $itemIndex")
        readAttributeInfo(source)
    }
}

fun readAttributeInfo(source: Source) {
    val attributeNameIndex = source.readUShort()
    println("      Attribute name index: $attributeNameIndex")
    val attributeLength = source.readUInt()
    println("      Attribute length: $attributeLength")
    val info = source.readByteArray(attributeLength.toInt())
    println("      Info: (binary: ${info.size})")
}

fun readMethodInfo(source: Source) {
    val accessFlags = source.readUShort()
    println("    Access flags: $accessFlags")
    val nameIndex = source.readUShort()
    println("    Name index: $nameIndex")
    val descriptorIndex = source.readUShort()
    println("    Descriptor index: $descriptorIndex")
    val attributesCount = source.readUShort()
    println("    Attributes count: $attributesCount")
    repeat(attributesCount.toInt()) { itemIndex ->
        println("    Attribute $itemIndex")
        readAttributeInfo(source)
    }
}
