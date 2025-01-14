package it.krzeminski

import io.exoquery.pprint
import it.krzeminski.internal.*
import it.krzeminski.internal.String
import kotlinx.io.*
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip

fun main() {
    val firstClassFile: ByteArray = readFirstClassFileFromJar()
    val source = Buffer().apply { write(firstClassFile) }

    val (bytecodeVersion, kotlinMetadataVersion) = readVersions(source)
     println("Bytecode version: $bytecodeVersion")
     println("Kotlin metadata version: $kotlinMetadataVersion")
}

fun readFirstClassFileFromJar(): ByteArray {
    val zipFileSystem = FileSystem.SYSTEM
        .openZip("test-module-to-inspect/build/libs/test-module-to-inspect.jar".toPath())
    val classFilePath = zipFileSystem.list(".".toPath()).first { it.name.endsWith(".class") }
    return zipFileSystem.source(classFilePath).buffer().readByteArray()
}

fun readVersions(source: Source): Pair<kotlin.String, kotlin.String> {
    val classFile = readClassFile(source = source)
    // println(pprint(classFile, defaultHeight = 1000))
    val bytecodeVersion = "${classFile.majorVersion}.${classFile.minorVersion}"

    val runtimeVisibleAnnotations = classFile.attributes["RuntimeVisibleAnnotations"]!!

    val annotations = runtimeVisibleAnnotations.parse(classFile.constantPool)
    val kotlinMetadataVersion = (annotations["Lkotlin/Metadata;"]?.arguments["mv"] as ArrayArg)
        .items.joinToString(separator = ".")
    // println(pprint(annotations))
    return Pair(bytecodeVersion, kotlinMetadataVersion)
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
            add(readMethodInfo(source, constantPool))
        }
    }

    val attributesCount = source.readUShort()
    val attributes = buildMap {
        repeat(attributesCount.toInt()) {
            val attributeNameIndex = source.readUShort().toInt()
            val name = (constantPool[attributeNameIndex - 1] as Utf8).string
            put(name, readAttributeInfo(source))
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
    val attributeLength = source.readUInt()
    val info = source.readByteArray(attributeLength.toInt())
    return AttributeInfo(
        info = info,
    )
}

fun readMethodInfo(source: Source, constantPool: List<ConstantPoolStruct>): MethodInfo {
    val accessFlags = source.readUShort().toInt()
    val nameIndex = source.readUShort().toInt()
    val descriptorIndex = source.readUShort().toInt()
    val attributesCount = source.readUShort()
    val attributes = buildMap {
        repeat(attributesCount.toInt()) {
            val attributeNameIndex = source.readUShort().toInt()
            val name = (constantPool[attributeNameIndex - 1] as Utf8).string
            put(name, readAttributeInfo(source))
        }
    }
    return MethodInfo(
        accessFlags = accessFlags,
        nameIndex = nameIndex,
        descriptorIndex = descriptorIndex,
        attributes = attributes,
    )
}

fun AttributeInfo.parse(constantPool: List<ConstantPoolStruct>): Map<kotlin.String, ParsedAttributeInfo> {
    val infoBuffer = Buffer().apply { write(this@parse.info) }
    val noOfAnnotations = infoBuffer.readUShort().toInt()
    val map = buildMap {
        repeat(noOfAnnotations) {
            val nameIndex = infoBuffer.readUShort().toInt()
            val name = (constantPool[nameIndex - 1] as Utf8).string
            val arguments = readArguments(infoBuffer, constantPool)
            val parsedAttributeInfo = ParsedAttributeInfo(
                arguments = arguments,
            )

            put(name, parsedAttributeInfo)
        }
    }
    require(infoBuffer.exhausted()) {
        "There's still some data to read!"
    }
    return map
}

fun readArguments(infoBuffer: Buffer, constantPool: List<ConstantPoolStruct>): Map<kotlin.String, ArgumentInfo> {
    val noOfArguments = infoBuffer.readUShort().toInt()
    // println("No of arguments: $noOfArguments")
    return buildMap {
        repeat (noOfArguments) {
            val argNameIndex = infoBuffer.readUShort().toInt()
            val argName = (constantPool[argNameIndex - 1] as Utf8).string
            // println("Arg name: $argName")

            val fieldDescriptor = infoBuffer.readByte().toInt().toChar()
            when (fieldDescriptor) {
                '[' -> {
                    val noOfArrayItems = infoBuffer.readUShort().toInt()
                    // println("No of array items: $noOfArrayItems")
                    val array = ArrayArg(items = buildList {
                        repeat(noOfArrayItems) {
                            val fieldDescriptor = infoBuffer.readByte().toInt().toChar()
                            // println("Field descriptor: $fieldDescriptor")
                            when (fieldDescriptor) {
                                'I' -> {
                                    val valueIndex = infoBuffer.readUShort().toInt()
                                    val value = (constantPool[valueIndex - 1] as Integer).value
                                    // println("Value: $value")
                                    add(value)
                                }
                                // I think a string?
                                's' -> {
                                    val valueIndex = infoBuffer.readUShort().toInt()
                                    val string = (constantPool[valueIndex - 1] as Utf8).string
                                    // println("String: $string")
                                    add(string)
                                }
                            }
                        }
                    })
                    put(argName, array)
                }
                'I' -> {
                    val valueIndex = infoBuffer.readUShort().toInt()
                    val value = (constantPool[valueIndex - 1] as Integer).value
                    // println("Value: $value")
                    put(argName, IntArg(value = value))
                }
            }
        }
    }
}
