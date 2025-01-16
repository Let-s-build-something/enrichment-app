package base.utils

object LinkUtils {
    /** Email address pattern, same as android.util.Patterns.EMAIL_ADDRESS */
    val emailRegex = """[a-zA-Z0-9+._%-+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9-]{0,25})+""".toRegex()

    /** URL pattern, no HTTP or HTTPS needed */
    val urlRegex = """(?<=^|[^a-z@])[^\s@]+\.[a-z]+(?=${'$'}|[^a-z@])""".toRegex()

    /** URL pattern, no HTTP or HTTPS needed */
    val phoneNumberRegex = """\+?\d{1,4}?[\s-]?\(?(\d{1,4})\)?[\s-]?\d{1,4}[\s-]?\d{1,4}[\s-]?\d{1,9}""".toRegex()
}

expect fun shareLink(title: String, link: String): Boolean

expect fun shareMessage(media: List<String>, messageContent: String = ""): Boolean

expect fun openLink(link: String): Boolean

expect fun openFile(path: String?)

expect fun downloadFiles(data: Map<String, ByteArray>): Boolean
