package base.utils

expect fun shareLink(title: String, link: String): Boolean

expect fun shareMessage(media: List<String>, messageContent: String = ""): Boolean

expect fun openLink(link: String): Boolean

expect fun openFile(path: String?)

expect fun downloadFiles(data: Map<String, ByteArray>): Boolean
