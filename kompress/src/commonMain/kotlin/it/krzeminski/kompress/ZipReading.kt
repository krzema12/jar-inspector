package it.krzeminski.kompress

import okio.Buffer

fun readZip(byteArray: ByteArray): Byte {
    val buffer = Buffer().apply { write(byteArray) }
    return buffer.readByte()
}
