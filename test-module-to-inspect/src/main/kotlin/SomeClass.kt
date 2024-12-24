import java.time.Instant
import java.time.ZoneId
import java.util.Date

/**
 * Some dummy code to test reading the compiled .class file.
 */
class SomeClass {
    fun someMethod(instant: Instant): Date {
        val someText = "foobar"
        someText.chars().sorted().max().orElseThrow()
        return Date.from(instant.atZone(ZoneId.systemDefault()).toInstant())
    }
}
