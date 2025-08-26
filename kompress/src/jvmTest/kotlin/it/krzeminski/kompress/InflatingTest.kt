package it.krzeminski.kompress

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import java.util.zip.Deflater
import java.io.ByteArrayOutputStream

class InflateTestCase(
    val name: String,
    val originalData: ByteArray
) {
    override fun toString(): String = name
}

class InflatingTest : FunSpec({
    val testCases = listOf(
        InflateTestCase(
            name = "empty byte array",
            originalData = byteArrayOf()
        ),
        InflateTestCase(
            name = "single byte",
            originalData = byteArrayOf(42)
        ),
        InflateTestCase(
            name = "simple string",
            originalData = "Hello".encodeToByteArray()
        ),
        InflateTestCase(
            name = "repeated characters - highly compressible",
            originalData = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".encodeToByteArray()
        ),
        InflateTestCase(
            name = "longer mixed text",
            originalData = "The quick brown fox jumps over the lazy dog. This sentence contains most letters of the alphabet.".encodeToByteArray()
        ),
        InflateTestCase(
            name = "binary data",
            originalData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte(), 0xFC.toByte())
        ),
        InflateTestCase(
            name = "highly repetitive pattern - maximum compression potential",
            originalData = run {
                val pattern = "abcabc".encodeToByteArray()
                ByteArray(pattern.size * 100) { i -> pattern[i % pattern.size] }
            }
        ),
        InflateTestCase(
            name = "large text with repeated phrases",
            originalData = buildString {
                repeat(50) {
                    append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
                }
                append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
            }.encodeToByteArray()
        ),
        InflateTestCase(
            name = "alternating pattern",
            originalData = "ABABABABABABABABABABABABABABABABABABAB".encodeToByteArray()
        ),
        InflateTestCase(
            name = "pseudo-random data - low compressibility",
            originalData = ByteArray(1000) { (it * 31 + 17).toByte() }
        ),
        InflateTestCase(
            name = "all ASCII characters",
            originalData = ByteArray(256) { it.toByte() }
        ),
        InflateTestCase(
            name = "newlines and special characters",
            originalData = "Line 1\nLine 2\r\nLine 3\tTabbed\n\n\nMultiple newlines".encodeToByteArray()
        ),
        InflateTestCase(
            name = "JSON-like structure with repetitive keys",
            originalData = """
                {"name":"John","age":30,"name":"Jane","age":25,"name":"Bob","age":35,"name":"Alice","age":28}
                {"name":"Charlie","age":32,"name":"Diana","age":27,"name":"Eve","age":31,"name":"Frank","age":29}
            """.trimIndent().encodeToByteArray()
        )
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
