package it.krzeminski.internal

data class ClassFile(
    val majorVersion: UShort,
    val minorVersion: UShort,
//    val constantPool: List<ConstantPoolStruct>,
//    val accessFlags: UShort,
//    val thisClass: UShort,
//    val superclass: UShort,
//    val interfaces: List<UShort>,
//    val fields: List<FieldInfo>,
//    val methods: List<MethodInfo>,
//    val attributes: Map<kotlin.String, AttributeInfo>,
)

sealed interface ConstantPoolStruct

data class Utf8(val string: kotlin.String) : ConstantPoolStruct

data class Integer(val value: Int) : ConstantPoolStruct

data class Class(val nameIndex: Int) : ConstantPoolStruct

data class String(val stringIndex: Int) : ConstantPoolStruct

data class Fieldref(
    val classIndex: Int,
    val nameAndTypeIndex: Int,
) : ConstantPoolStruct

data class Methodref(
    val classIndex: Int,
    val nameAndTypeIndex: Int,
) : ConstantPoolStruct

data class InterfaceMethodref(
    val classIndex: Int,
    val nameAndTypeIndex: Int,
) : ConstantPoolStruct

data class NameAndType(
    val nameIndex: Int,
    val descriptorIndex: Int,
) : ConstantPoolStruct

data class FieldInfo(
    val accessFlags: Int,
    val nameIndex: Int,
    val descriptorIndex: Int,
    val attributes: List<AttributeInfo>,
) : ConstantPoolStruct

data class AttributeInfo(
    val info: ByteArray,
)

data class MethodInfo(
    val accessFlags: Int,
    val nameIndex: Int,
    val descriptorIndex: Int,
    val attributes: Map<kotlin.String, AttributeInfo>,
)

data class ParsedAttributeInfo(
    val arguments: Map<kotlin.String, ArgumentInfo>,
)

sealed interface ArgumentInfo

data class ArrayArg(
    val items: List<Any>,
) : ArgumentInfo

data class IntArg(val value: Int) : ArgumentInfo

