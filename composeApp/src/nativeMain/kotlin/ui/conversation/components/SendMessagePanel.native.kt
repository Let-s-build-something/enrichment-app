package ui.conversation.components

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSMakeRange
import platform.Foundation.NSString
import platform.Foundation.NSStringEnumerationByComposedCharacterSequences
import platform.Foundation.enumerateSubstringsInRange

@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(ExperimentalForeignApi::class)
fun graphemeMatches(input: String): List<String> {
    val result = mutableListOf<String>()
    val nsString = input as NSString
    val fullRange = NSMakeRange(0u, nsString.length)

    nsString.enumerateSubstringsInRange(fullRange, NSStringEnumerationByComposedCharacterSequences) { substring, _, _, _ ->
        if (substring != null) {
            result.add(substring)
        }
    }

    return result
}