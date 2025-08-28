package it.krzeminski.kompress

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
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
        actual shouldBe mapOf(
            "test-dir/" to emptyArray<Byte>(),
            "test-dir/some-file1.txt" to "Some contents!\n".toByteArray(),
            "test-dir/another-dir/" to emptyArray<Byte>(),
            "test-dir/another-dir/some-file2.txt" to "Another contents?\n".toByteArray()
        )
    }

    test("library JAR") {
        // Given
        val testZipInputStream: InputStream = this::class.java.classLoader.getResourceAsStream("snakeyaml-engine-kmp-jvm-3.2.0.jar")!!
        val bytes = testZipInputStream.readAllBytes()
        testZipInputStream.close()

        // When
        val actual = readZip(bytes)

        // Then
        actual shouldContainKey "ble"
    }
})
