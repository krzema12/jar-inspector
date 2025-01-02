package it.krzeminski.internal

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
    val attributeNameIndex: Int,
    val info: ByteArray,
)

data class MethodInfo(
    val accessFlags: Int,
    val nameIndex: Int,
    val descriptorIndex: Int,
    val attributes: List<AttributeInfo>,
)
