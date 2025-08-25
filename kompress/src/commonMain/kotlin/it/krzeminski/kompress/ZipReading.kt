package it.krzeminski.kompress

import okio.Buffer

fun readZip(byteArray: ByteArray): Byte {
    val buffer = Buffer().apply { write(byteArray) }
    val bufferForReadingEndOfCentral = buffer.copy()

    val endOfCentralPos = bufferForReadingEndOfCentral.findEndOfCentral()
    println("End of central: $endOfCentralPos")

    val magicNumber = bufferForReadingEndOfCentral.readInt()
    println("Magic number: ${magicNumber.toHexString()}")
    val numberOfThisDisk = bufferForReadingEndOfCentral.readShortLe()
    println("Number of this disk: $numberOfThisDisk")
    val diskWhereCentralDirectoryStarts = bufferForReadingEndOfCentral.readShortLe()
    println("Disk where central directory starts: $diskWhereCentralDirectoryStarts")
    val numberOfCentralDirectoryRecordsOnThisDisk = bufferForReadingEndOfCentral.readShortLe()
    println("Number of central directory records on this disk: $numberOfCentralDirectoryRecordsOnThisDisk")
    val totalNumberOfCentralDirectoryRecords = bufferForReadingEndOfCentral.readShortLe()
    println("Total number of central directory records: $totalNumberOfCentralDirectoryRecords")
    val sizeOfCentralDirectory = bufferForReadingEndOfCentral.readIntLe()
    println("Size of central directory: $sizeOfCentralDirectory")
    val offsetOfStartOfCentralDirectory = bufferForReadingEndOfCentral.readIntLe()
    println("Offset of start of central directory: $offsetOfStartOfCentralDirectory")
    val commentLength = bufferForReadingEndOfCentral.readShortLe()
    println("Comment length: $commentLength")
    val comment = bufferForReadingEndOfCentral.readUtf8(commentLength.toLong())
    println("Comment: $comment")

    return buffer.readByte()
}

private fun Buffer.findEndOfCentral(): Long {
    var pos: Long = 0
    while (!this.exhausted()) {
        if (peek().readInt() == 0x504B0506) {
            return pos
        }
        pos += 1
        this.skip(1)
    }
    error("Reached EOF before finding EOCD")
}
