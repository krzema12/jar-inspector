package it.krzeminski.kompress

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

actual fun inflateWithJdk(data: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)

    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)

    try {
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0) {
                break
            }
            outputStream.write(buffer, 0, count)
        }
    } finally {
        inflater.end()
    }

    return outputStream.toByteArray()
}
