import jszip.JSZip
import jszip.load
import kotlin.collections.forEachIndexed
import org.khronos.webgl.Uint8Array

actual suspend fun readFirstClassFileFromJar(byteArray: ByteArray): ByteArray {
    println("In readFirstClassFileFromJar")
    val uint8Array = Uint8Array(byteArray.toTypedArray())
    byteArray.forEachIndexed { index, byte -> uint8Array[index] = byte }
    println("Converted to Uint8Array")
    val files = JSZip().load(uint8Array, options = null)
    println("Loaded with JSZip:")
    println(files.files)
//    val uint8Array = byteArray.toUint8Array()
//    println("Converted byteArray")
//    val responseFromPako = Pako.inflate(uint8Array, toStringOptions)
//    println("Got a response from pako")
//    return responseFromPako.toByteArray()
    return ByteArray(0)
}

private val toStringOptions: JsAny = js("({to: 'string'})")

//private fun ByteArray.toUint8Array(): Uint8Array {
//    val result = Uint8Array(size)
//    forEachIndexed { index, byte ->
//        result[index] = byte
//    }
//    return result
//}
//
//@Suppress("UnusedPrivateMember") // False positive
//@JsModule("pako")
//private external object Pako {
//    fun inflate(data: Uint8Array, options: JsAny): String
//}
