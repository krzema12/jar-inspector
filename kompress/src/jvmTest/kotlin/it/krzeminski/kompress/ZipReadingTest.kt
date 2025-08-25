package it.krzeminski.kompress

import io.kotest.core.spec.style.FunSpec
import java.io.InputStream

class ZipReadingTest : FunSpec({
    test("simple ZIP") {
        // Given
        val testZipInputStream: InputStream = this::class.java.classLoader.getResourceAsStream("test-zip.zip")!!
        val bytes = testZipInputStream.readAllBytes()
        testZipInputStream.close()

        // When
        val actual = readZip(bytes)

        // Then
        // TODO: return a list of files, and assert on something meaningful
        actual == 0x50.toByte()
    }
})
