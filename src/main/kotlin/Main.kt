package it.krzeminski

import io.exoquery.pprint
import it.krzeminski.internal.*
import it.krzeminski.internal.String
import kotlinx.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

fun main() {
    val classFileToRead = Path("test-module-to-inspect/build/classes/kotlin/main/SomeClass.class")
    val source = SystemFileSystem.source(classFileToRead).buffered()

    val classFile = readClassFile(source = source)
    println(pprint(classFile, defaultHeight = 1000))
}

fun readClassFile(source: Source): ClassFile {
    // https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
    val magic = source.readUInt()
    require(magic == 0xCAFEBABE.toUInt()) { "Magic is invalid: $magic" }

    val minorVersion = source.readUShort()
    val majorVersion = source.readUShort()

    val constantPoolCount = source.readUShort()

    val constantPool = buildList {
        repeat(constantPoolCount.toInt() - 1) {
            add(readConstantPoolEntry(source))
        }
    }

    val accessFlags = source.readUShort()
    val thisClass = source.readUShort()
    val superclass = source.readUShort()

    val interfacesCount = source.readUShort()
    val interfaces = buildList {
        repeat(interfacesCount.toInt()) {
            add(source.readUShort())
        }
    }

    val fieldsCount = source.readUShort()
    val fields = buildList {
        repeat(fieldsCount.toInt()) {
            add(readFieldInfo(source))
        }
    }

    val methodsCount = source.readUShort()
    val methods = buildList {
        repeat(methodsCount.toInt()) {
            add(readMethodInfo(source))
        }
    }

    val attributesCount = source.readUShort()
    val attributes = buildList {
        repeat(attributesCount.toInt()) {
            add(readAttributeInfo(source))
        }
    }

    require(source.exhausted()) {
        "There's still some data to read!"
    }

    return ClassFile(
        majorVersion = majorVersion,
        minorVersion = minorVersion,
        constantPool = constantPool,
        accessFlags = accessFlags,
        thisClass = thisClass,
        superclass = superclass,
        interfaces = interfaces,
        fields = fields,
        methods = methods,
        attributes = attributes,
    )
}

private fun readConstantPoolEntry(source: Source): ConstantPoolStruct {
    return when (val tag = source.readUByte().toInt()) {
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

private fun readUtf8Info(source: Source): Utf8 {
    val length = source.readUShort()
    val string = source.readString(length.toLong())
    return Utf8(
        string=string,
    )
}

private fun readInteger(source: Source): Integer {
    val value = source.readInt()
    return Integer(
        value = value,
    )
}

private fun readClass(source: Source): Class {
    val nameIndex = source.readUShort().toInt()
    return Class(
        nameIndex = nameIndex,
    )
}

private fun readString(source: Source): String {
    val stringIndex = source.readUShort().toInt()
    return String(
       stringIndex = stringIndex,
    )
}

private fun readFieldref(source: Source): Fieldref {
    val classIndex = source.readUShort().toInt()
    val nameAndTypeIndex = source.readUShort().toInt()
    return Fieldref(
        classIndex = classIndex,
        nameAndTypeIndex = nameAndTypeIndex,
    )
}

private fun readMethodref(source: Source): Methodref {
    val classIndex = source.readUShort().toInt()
    val nameAndTypeIndex = source.readUShort().toInt()
    return Methodref(
        classIndex = classIndex,
        nameAndTypeIndex = nameAndTypeIndex,
    )
}

private fun readInterfaceMethodref(source: Source): InterfaceMethodref {
    val classIndex = source.readUShort().toInt()
    val nameAndTypeIndex = source.readUShort().toInt()
    return InterfaceMethodref(
        classIndex = classIndex,
        nameAndTypeIndex = nameAndTypeIndex,
    )
}

private fun readNameAndType(source: Source): NameAndType {
    val nameIndex = source.readUShort().toInt()
    val descriptorIndex = source.readUShort().toInt()
    return NameAndType(
        nameIndex = nameIndex,
        descriptorIndex = descriptorIndex,
    )
}

private fun readFieldInfo(source: Source): FieldInfo {
    val accessFlags = source.readUShort().toInt()
    val nameIndex = source.readUShort().toInt()
    val descriptorIndex = source.readUShort().toInt()
    val attributesCount = source.readUShort()
    val attributes = buildList {
        repeat(attributesCount.toInt()) {
            add(readAttributeInfo(source))
        }
    }
    return FieldInfo(
        accessFlags = accessFlags,
        nameIndex = nameIndex,
        descriptorIndex = descriptorIndex,
        attributes = attributes,
    )
}

fun readAttributeInfo(source: Source): AttributeInfo {
    val attributeNameIndex = source.readUShort().toInt()
    val attributeLength = source.readUInt()
    val info = source.readByteArray(attributeLength.toInt())
    return AttributeInfo(
        attributeNameIndex = attributeNameIndex,
        info = info,
    )
}

fun readMethodInfo(source: Source): MethodInfo {
    val accessFlags = source.readUShort().toInt()
    val nameIndex = source.readUShort().toInt()
    val descriptorIndex = source.readUShort().toInt()
    val attributesCount = source.readUShort()
    val attributes = buildList {
        repeat(attributesCount.toInt()) {
            add(readAttributeInfo(source))
        }
    }
    return MethodInfo(
        accessFlags = accessFlags,
        nameIndex = nameIndex,
        descriptorIndex = descriptorIndex,
        attributes = attributes,
    )
}
