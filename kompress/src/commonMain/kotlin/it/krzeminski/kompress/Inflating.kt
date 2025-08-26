package it.krzeminski.kompress

class BitReader(private val data: ByteArray) {
    private var byteIndex = 0
    private var bitIndex = 0

    fun hasMoreData(): Boolean = byteIndex < data.size

    fun readBit(): Int {
        if (byteIndex >= data.size) return 0

        val bit = (data[byteIndex].toInt() and 0xFF) shr bitIndex and 1
        bitIndex++

        if (bitIndex >= 8) {
            bitIndex = 0
            byteIndex++
        }

        return bit
    }

    fun readBits(count: Int): Int {
        var result = 0
        for (i in 0 until count) {
            result = result or (readBit() shl i)
        }
        return result
    }

    fun alignToByte() {
        if (bitIndex > 0) {
            bitIndex = 0
            byteIndex++
        }
    }

    fun readByte(): Int {
        alignToByte()
        return if (byteIndex < data.size) {
            data[byteIndex++].toInt() and 0xFF
        } else {
            0
        }
    }

    fun readBytes(count: Int): ByteArray {
        alignToByte()
        val result = ByteArray(count)
        for (i in 0 until count) {
            result[i] = if (byteIndex < data.size) data[byteIndex++] else 0
        }
        return result
    }
}

// Fixed Huffman code tables for DEFLATE
private val fixedLiteralCodes = IntArray(288) { symbol ->
    when (symbol) {
        in 0..143 -> 8
        in 144..255 -> 9
        in 256..279 -> 7
        in 280..287 -> 8
        else -> 0
    }
}

private val fixedDistanceCodes = IntArray(32) { 5 }

// Length and distance tables
private val lengthBase = intArrayOf(
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
    35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
)

private val lengthExtraBits = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
    3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
)

private val distanceBase = intArrayOf(
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
    257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
)

private val distanceExtraBits = intArrayOf(
    0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
    7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
)

private fun decodeFixedHuffman(bitReader: BitReader, isLiteral: Boolean): Int {
    var code = 0
    var bits = 0

    if (isLiteral) {
        // Decode literal/length symbol using fixed Huffman table
        while (bits < 9) {
            code = (code shl 1) or bitReader.readBit()
            bits++

            when (bits) {
                7 -> if (code in 0..23) return code + 256  // 0000000..0010111 -> 256-279
                8 -> if (code in 48..191) return code - 48  // 00110000..10111111 -> 0-143
                9 -> if (code in 400..511) return code - 256  // 110010000..111111111 -> 144-255
            }
        }

        // Handle symbols 280-287 (9 bits, 11000000x - 11000111x)
        if (code in 384..399) return code - 104
    } else {
        // Distance codes are all 5 bits (0-31)
        return bitReader.readBits(5)
    }

    error("Invalid Huffman code")
}

fun inflate(data: ByteArray): ByteArray {
    if (data.isEmpty()) {
        return byteArrayOf()
    }

    // Parse ZLIB header (2 bytes)
    if (data.size < 2) {
        error("Invalid DEFLATE data: too short")
    }

    val cmf = data[0].toInt() and 0xFF
    val flg = data[1].toInt() and 0xFF

    // Verify ZLIB header
    val compressionMethod = cmf and 0x0F
    if (compressionMethod != 8) {
        error("Unsupported compression method: $compressionMethod")
    }

    // Skip ZLIB header and checksum (2 bytes at start, 4 bytes at end)
    val deflateData = data.sliceArray(2 until data.size - 4)

    val bitReader = BitReader(deflateData)
    val output = mutableListOf<Byte>()

    var lastBlock = false
    while (!lastBlock && bitReader.hasMoreData()) {
        lastBlock = bitReader.readBit() == 1
        val blockType = bitReader.readBits(2)

        when (blockType) {
            0 -> { // Uncompressed block
                bitReader.alignToByte()
                val len = bitReader.readByte() or (bitReader.readByte() shl 8)
                val nlen = bitReader.readByte() or (bitReader.readByte() shl 8)

                if (len != (nlen xor 0xFFFF)) {
                    error("Invalid uncompressed block")
                }

                output.addAll(bitReader.readBytes(len).toList())
            }

            1 -> { // Fixed Huffman
                while (true) {
                    val symbol = decodeFixedHuffman(bitReader, true)

                    if (symbol < 256) {
                        output.add(symbol.toByte())
                    } else if (symbol == 256) {
                        break // End of block
                    } else if (symbol <= 285) {
                        // Length/distance pair
                        val lengthIndex = symbol - 257
                        val length = lengthBase[lengthIndex] + bitReader.readBits(lengthExtraBits[lengthIndex])

                        val distanceSymbol = decodeFixedHuffman(bitReader, false)
                        val distance = distanceBase[distanceSymbol] + bitReader.readBits(distanceExtraBits[distanceSymbol])

                        // Copy previous bytes
                        repeat(length) {
                            output.add(output[output.size - distance])
                        }
                    }
                }
            }

            2 -> error("Dynamic Huffman not implemented yet")
            3 -> error("Reserved block type")
        }
    }

    return output.toByteArray()
}
