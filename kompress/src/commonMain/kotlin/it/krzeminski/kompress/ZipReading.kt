package it.krzeminski.kompress

import okio.Buffer
import kotlin.math.PI

data class FileProps(
    val offset: Int,
    val compressionMethod: Short,
    val compressedSize: Int,
)

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

    val bufferForReadingCentral = buffer.copy()
    bufferForReadingCentral.skip(offsetOfStartOfCentralDirectory.toLong())

    val fileNameToLocalHeaderOffset = mutableMapOf<String, FileProps>()

    repeat(numberOfCentralDirectoryRecordsOnThisDisk.toInt()) {
        println("============ NEW CENTARL DIR ENTRY ==============")
        val magicNumberCentral = bufferForReadingCentral.readInt()
        println("Magic number central: ${magicNumberCentral.toHexString()}")
        val versionMadeBy = bufferForReadingCentral.readShortLe()
        println("Version made by: $versionMadeBy")
        val versionNeededToExtract = bufferForReadingCentral.readShortLe()
        println("Version needed to extract: $versionNeededToExtract")
        val generalPurposeFlag = bufferForReadingCentral.readShortLe()
        println("General purpose flag: $generalPurposeFlag")
        val compressionMethod = bufferForReadingCentral.readShortLe()
        println("Compression method: $compressionMethod")
        val lastModFileTime = bufferForReadingCentral.readShortLe()
        println("Last mod file time: $lastModFileTime")
        val lastModFileDate = bufferForReadingCentral.readShortLe()
        println("Last mod file date: $lastModFileDate")
        val crc32 = bufferForReadingCentral.readIntLe()
        println("CRC32: $crc32")
        val compressedSize = bufferForReadingCentral.readIntLe()
        println("Compressed size: $compressedSize")
        val uncompressedSize = bufferForReadingCentral.readIntLe()
        println("Uncompressed size: $uncompressedSize")
        val fileNameLength = bufferForReadingCentral.readShortLe()
        println("File name length: $fileNameLength")
        val extraFieldLength = bufferForReadingCentral.readShortLe()
        println("Extra field length: $extraFieldLength")
        val fileCommentLength = bufferForReadingCentral.readShortLe()
        println("File comment length: $fileCommentLength")
        val diskNumberWhereFileStarts = bufferForReadingCentral.readShortLe()
        println("Disk number where file starts: $diskNumberWhereFileStarts")
        val internalFileAttributes = bufferForReadingCentral.readShortLe()
        println("Internal file attributes: $internalFileAttributes")
        val externalFileAttributes = bufferForReadingCentral.readIntLe()
        println("External file attributes: $externalFileAttributes")
        val offsetOfLocalHeader = bufferForReadingCentral.readIntLe()
        println("Offset of local header: $offsetOfLocalHeader")
        val fileName = bufferForReadingCentral.readUtf8(fileNameLength.toLong())
        println("File name: $fileName")
        val extraField = bufferForReadingCentral.readByteArray(extraFieldLength.toLong())
        println("Extra field: $extraField")
        val fileComment = bufferForReadingCentral.readUtf8(fileCommentLength.toLong())
        println("File comment: $fileComment")

        fileNameToLocalHeaderOffset[fileName] = FileProps(
            offset = offsetOfLocalHeader,
            compressionMethod = compressionMethod,
            compressedSize = compressedSize,
        )
    }

    fileNameToLocalHeaderOffset.forEach { (fileName, fileProps) ->
        println("")
        println("### Reading $fileName at position ${fileProps.offset} - compression method: ${fileProps.compressionMethod}...")
        val bufferForReadingFile = buffer.copy()
        bufferForReadingFile.skip(fileProps.offset.toLong())
        val magicNumber = bufferForReadingFile.readInt()
        println("Magic number: ${magicNumber.toHexString()}")
        bufferForReadingFile.skip(22) // Local header - we don't rely on it, we use only the central dir.
        val fileNameLength = bufferForReadingFile.readShortLe()
        println("File name length: $fileNameLength")
        val localExtraFieldsLength = bufferForReadingFile.readShortLe()
        println("Local extra fields length: $localExtraFieldsLength")
        val fileName = bufferForReadingFile.readUtf8(fileNameLength.toLong())
        println("File name: $fileName")
        bufferForReadingFile.skip(localExtraFieldsLength.toLong())

        println("Compressed size: ${fileProps.compressedSize}")
        if (fileProps.compressedSize != 0) {
            val fileData = bufferForReadingFile.readByteArray(fileProps.compressedSize.toLong())
            println("Compression method: ${fileProps.compressionMethod}")
            if (fileProps.compressionMethod == 0.toShort()) {
                println("File data: ${String(fileData)}")
            } else {
                println("File data: ENCODED (TODO)")
            }
        } else {
            println("0 size of compressed data")
        }
    }

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
