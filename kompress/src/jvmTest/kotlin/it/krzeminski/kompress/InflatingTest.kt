package it.krzeminski.kompress

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import java.util.zip.Deflater
import java.io.ByteArrayOutputStream

class InflateTestCase(
    val name: String,
    val originalData: ByteArray,
    val expectedDeflatedData: ByteArray
) : WithDataTestName {
    override fun dataTestName(): String = name
}

class InflatingTest : FunSpec({
    val testCases = listOf(
        InflateTestCase(
            name = "empty byte array",
            originalData = byteArrayOf(),
            expectedDeflatedData = byteArrayOf(120, -100, 3, 0, 0, 0, 0, 1)
        ),
        InflateTestCase(
            name = "single byte",
            originalData = byteArrayOf(42),
            expectedDeflatedData = byteArrayOf(120, -100, -45, 2, 0, 0, 43, 0, 43)
        ),
        InflateTestCase(
            name = "simple string",
            originalData = "Hello".encodeToByteArray(),
            expectedDeflatedData = byteArrayOf(120, -100, -13, 72, -51, -55, -55, 7, 0, 5, -116, 1, -11)
        ),
        InflateTestCase(
            name = "repeated characters - highly compressible",
            originalData = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".encodeToByteArray(),
            expectedDeflatedData = byteArrayOf(120, -100, 115, 116, 36, 14, 0, 0, -48, 92, 10, 41)
        ),
        InflateTestCase(
            name = "longer mixed text",
            originalData = "The quick brown fox jumps over the lazy dog. This sentence contains most letters of the alphabet.".encodeToByteArray(),
            expectedDeflatedData = byteArrayOf(120, -100, 29, -53, -53, 17, -128, 32, 12, 5, -64, 86, 94, 5, 86, 99, 3,
                -128, 65, 80, 72, -112, -60, 111, -11, 58, -98, 119, 118, 76, -124, 109, -49, 97, -123, -17, 114, 50,
                -94, 92, 88, -10, -38, 20, 114, 80, -121, 125, 92, -36, 115, 99, -110, 121, -64, -104, -78, 66, -119,
                -115, 56, 16, -126, -80, -71, -52, -118, 42, 106, 40, 100, 70, -3, 107, -15, 79, -82, -76, -28, 60, -39,
                -16, 2, -50, -127, 35, -97)
        ),
        InflateTestCase(
            name = "binary data",
            originalData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte(), 0xFC.toByte()),
            expectedDeflatedData = byteArrayOf(120, -100, 99, 96, 100, 98, -2, -1, -17, -17, 31, 0, 10, 22, 3, -3),
        ),
    )

    context("inflate deflated data") {
        withData(testCases) { testCase ->
            // Given
            val compressedData = deflateWithJdk(testCase.originalData)

            // When
            val result = inflate(compressedData)

            // Then
            result shouldBe testCase.originalData
        }
    }

    context("check test data: generated deflated data matches expected") {
        withData(testCases) { testCase ->
            val compressedData = deflateWithJdk(testCase.originalData)
            compressedData shouldBe testCase.expectedDeflatedData
        }
    }

    context("check reference impl from the JDK") {
        withData(testCases) { testCase ->
            inflateWithJdk(deflateWithJdk(testCase.originalData)) shouldBe testCase.originalData
        }
    }
})

private fun deflateWithJdk(input: ByteArray): ByteArray {
    val deflater = Deflater()
    deflater.setInput(input)
    deflater.finish()

    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)

    while (!deflater.finished()) {
        val count = deflater.deflate(buffer)
        outputStream.write(buffer, 0, count)
    }

    deflater.end()
    return outputStream.toByteArray()
}
