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

// Huffman tree node
sealed class HuffmanNode {
    data class Leaf(val symbol: Int) : HuffmanNode()
    data class Internal(val left: HuffmanNode, val right: HuffmanNode) : HuffmanNode()
}

private fun buildHuffmanTree(codeLengths: IntArray): HuffmanNode? {
    val maxLength = codeLengths.maxOrNull() ?: return null
    if (maxLength == 0) return null

    // Count codes of each length
    val lengthCounts = IntArray(maxLength + 1)
    for (length in codeLengths) {
        if (length > 0) lengthCounts[length]++
    }

    // Check for single symbol case
    if (lengthCounts.sum() == 1) {
        val symbol = codeLengths.indexOfFirst { it > 0 }
        return HuffmanNode.Internal(HuffmanNode.Leaf(symbol), HuffmanNode.Leaf(-1))
    }

    // Calculate first code for each length
    val firstCodes = IntArray(maxLength + 1)
    var code = 0
    for (length in 1..maxLength) {
        firstCodes[length] = code
        code += lengthCounts[length]
        code = code shl 1
    }

    // Create mutable root for building
    class MutableNode(var left: MutableNode? = null, var right: MutableNode? = null, var symbol: Int = -1)

    val root = MutableNode()

    // Build tree by inserting each symbol
    for (symbol in codeLengths.indices) {
        val length = codeLengths[symbol]
        if (length > 0) {
            val symbolCode = firstCodes[length]
            firstCodes[length]++

            // Navigate to the correct position in the tree
            var current = root
            for (bit in length - 1 downTo 0) {
                val goRight = (symbolCode shr bit) and 1 == 1

                if (bit == 0) {
                    // Last bit - place the symbol
                    if (goRight) {
                        current.right = MutableNode(symbol = symbol)
                    } else {
                        current.left = MutableNode(symbol = symbol)
                    }
                } else {
                    // Navigate deeper, creating nodes as needed
                    if (goRight) {
                        if (current.right == null) {
                            current.right = MutableNode()
                        }
                        current = current.right!!
                    } else {
                        if (current.left == null) {
                            current.left = MutableNode()
                        }
                        current = current.left!!
                    }
                }
            }
        }
    }

    // Convert mutable tree to immutable
    fun convertToImmutable(node: MutableNode?): HuffmanNode {
        return if (node == null || node.symbol != -1) {
            HuffmanNode.Leaf(node?.symbol ?: -1)
        } else {
            HuffmanNode.Internal(
                convertToImmutable(node.left),
                convertToImmutable(node.right)
            )
        }
    }

    return convertToImmutable(root)
}

private fun decodeHuffmanTree(bitReader: BitReader, tree: HuffmanNode?): Int {
    if (tree == null) error("Invalid Huffman tree")

    var current = tree
    while (current is HuffmanNode.Internal) {
        val bit = bitReader.readBit()
        current = if (bit == 1) current.right else current.left

        if (current is HuffmanNode.Leaf && current.symbol == -1) {
            error("Invalid Huffman code path")
        }
    }
    return (current as HuffmanNode.Leaf).symbol
}

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

    val bitReader = BitReader(data)
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

            2 -> { // Dynamic Huffman
                // Read header
                val hlit = bitReader.readBits(5) + 257  // number of literal/length codes
                val hdist = bitReader.readBits(5) + 1   // number of distance codes
                val hclen = bitReader.readBits(4) + 4   // number of code length codes

                // Code length alphabet order (permuted)
                val clcOrder = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
                val codeLengthCodeLengths = IntArray(19)

                // Read code lengths for the code length alphabet
                for (i in 0 until hclen) {
                    codeLengthCodeLengths[clcOrder[i]] = bitReader.readBits(3)
                }

                // Build code length tree
                val codeLengthTree = buildHuffmanTree(codeLengthCodeLengths)

                // Decode literal/length and distance code lengths
                val allCodeLengths = IntArray(hlit + hdist)
                var i = 0
                while (i < hlit + hdist) {
                    val symbol = decodeHuffmanTree(bitReader, codeLengthTree)

                    when (symbol) {
                        in 0..15 -> {
                            allCodeLengths[i++] = symbol
                        }
                        16 -> { // Repeat previous code length 3-6 times
                            val repeat = bitReader.readBits(2) + 3
                            val value = if (i > 0) allCodeLengths[i - 1] else 0
                            repeat(repeat) { allCodeLengths[i++] = value }
                        }
                        17 -> { // Repeat 0 for 3-10 times
                            val repeat = bitReader.readBits(3) + 3
                            repeat(repeat) { allCodeLengths[i++] = 0 }
                        }
                        18 -> { // Repeat 0 for 11-138 times
                            val repeat = bitReader.readBits(7) + 11
                            repeat(repeat) { allCodeLengths[i++] = 0 }
                        }
                    }
                }

                // Split into literal/length and distance code lengths
                val literalLengths = allCodeLengths.sliceArray(0 until hlit)
                val distanceLengths = allCodeLengths.sliceArray(hlit until hlit + hdist)

                // Build trees
                val literalTree = buildHuffmanTree(literalLengths)
                val distanceTree = buildHuffmanTree(distanceLengths)

                // Decode block data using dynamic trees
                while (true) {
                    val symbol = decodeHuffmanTree(bitReader, literalTree)

                    if (symbol < 256) {
                        output.add(symbol.toByte())
                    } else if (symbol == 256) {
                        break // End of block
                    } else if (symbol <= 285) {
                        // Length/distance pair
                        val lengthIndex = symbol - 257
                        val length = lengthBase[lengthIndex] + bitReader.readBits(lengthExtraBits[lengthIndex])

                        val distanceSymbol = decodeHuffmanTree(bitReader, distanceTree)
                        val distance = distanceBase[distanceSymbol] + bitReader.readBits(distanceExtraBits[distanceSymbol])

                        // Copy previous bytes
                        repeat(length) {
                            output.add(output[output.size - distance])
                        }
                    }
                }
            }
            3 -> error("Reserved block type")
        }
    }

    return output.toByteArray()
}
