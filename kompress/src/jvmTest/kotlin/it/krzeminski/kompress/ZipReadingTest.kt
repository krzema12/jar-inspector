package it.krzeminski.kompress

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import okio.Buffer
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

    test("JAR: snakeyaml-engine-kmp-jvm-3.2.0") {
        // Given
        val testZipInputStream: InputStream = this::class.java.classLoader.getResourceAsStream("snakeyaml-engine-kmp-jvm-3.2.0.jar")!!
        val bytes = testZipInputStream.readAllBytes()
        testZipInputStream.close()

        // When
        val actual = readZip(bytes)

        // Then
        actual shouldContainKey "it/krzeminski/snakeyaml/engine/kmp/api/ConstructNode.class"
        val classFileData = actual["it/krzeminski/snakeyaml/engine/kmp/api/ConstructNode.class"]

        val source = Buffer().apply { write(classFileData!!) }
        val magic = source.readInt()
        magic.toUInt() shouldBe 0xCAFEBABE.toUInt()
        val minorVersion = source.readShort()
        val majorVersion = source.readShort()
        majorVersion shouldBe 52
        minorVersion shouldBe 0
    }

    test("JAR: guava-11.0.1") {
        // Given
        val testZipInputStream: InputStream = this::class.java.classLoader.getResourceAsStream("guava-11.0.1.jar")!!
        val bytes = testZipInputStream.readAllBytes()
        testZipInputStream.close()

        // When
        val actual = readZip(bytes)

        // Then
        actual shouldContainKey "it/krzeminski/snakeyaml/engine/kmp/api/ConstructNode.class"
        val classFileData = actual["it/krzeminski/snakeyaml/engine/kmp/api/ConstructNode.class"]

        val source = Buffer().apply { write(classFileData!!) }
        val magic = source.readInt()
        magic.toUInt() shouldBe 0xCAFEBABE.toUInt()
        val minorVersion = source.readShort()
        val majorVersion = source.readShort()
        majorVersion shouldBe 52
        minorVersion shouldBe 0
    }
})
