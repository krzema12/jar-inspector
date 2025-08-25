package it.krzeminski.kompress

import okio.Buffer

fun readZip(byteArray: ByteArray) {
    val buffer = Buffer().apply { write(byteArray) }
    buffer.
}