package components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import augmy.interactive.shared.ui.theme.LocalTheme
import base.utils.LinkUtils.emailRegex
import base.utils.LinkUtils.phoneNumberRegex
import base.utils.LinkUtils.urlRegex


/**
 * Clickable text supporting <a href> HTML tags and can also match email, URL addresses, and phone numbers if needed
 * @param linkStyles styles applied to the found link
 * @param matchEmail whether an email should be considered a link
 * @param matchPhone whether a phone number should be considered a link
 * @param matchUrl whether an url should be considered a link
 * @param onLinkClicked called whenever a link was clicked, propagating the link including prefix for phone number and email
 */
@Composable
fun buildAnnotatedLinkString(
    text: String,
    linkStyles: TextLinkStyles = LocalTheme.current.styles.link,
    matchEmail: Boolean = true,
    matchPhone: Boolean = true,
    matchUrl: Boolean = true,
    onLinkClicked: (link: String) -> Unit
)= buildAnnotatedString {
    val anchorRegex = Regex("""<a\s+href="([^"]+)">([^<]+)<\/a>""")
    val protectedRanges = mutableListOf<IntRange>()
    val matches = anchorRegex.findAll(text).toList()
    var lastIndex = 0

    // Step 1: Identify <a> tag ranges to protect
    anchorRegex.findAll(text).forEach { match ->
        protectedRanges.add(match.range)
    }

    // Step 2: Replace raw emails/phones/URLs only if outside <a> tags
    val safeBuilder = StringBuilder()

    for (match in matches) {
        // Step 1: Raw chunk before the <a> block
        if (lastIndex < match.range.first) {
            val rawChunk = text.substring(lastIndex, match.range.first)

            val processedChunk = rawChunk
                .let {
                    var replaced = it
                    if (matchEmail) {
                        replaced = replaced.replace(emailRegex) { m ->
                            "<a href=\"mailto:${m.value}\">${m.value}</a>"
                        }
                    }
                    if (matchPhone) {
                        replaced = replaced.replace(phoneNumberRegex) { m ->
                            "<a href=\"tel:${m.value}\">${m.value}</a>"
                        }
                    }
                    if (matchUrl) {
                        replaced = replaced.replace(urlRegex) { m ->
                            "<a href=\"${m.value}\">${m.value}</a>"
                        }
                    }
                    replaced
                }

            safeBuilder.append(processedChunk)
        }

        // Step 2: Keep the <a> block as-is
        safeBuilder.append(match.value)
        lastIndex = match.range.last + 1
    }

    // Step 3: Process the trailing raw text
    if (lastIndex < text.length) {
        val tail = text.substring(lastIndex)

        val processedTail = tail
            .let {
                var replaced = it
                if (matchEmail) {
                    replaced = replaced.replace(emailRegex) { m ->
                        "<a href=\"mailto:${m.value}\">${m.value}</a>"
                    }
                }
                if (matchPhone) {
                    replaced = replaced.replace(phoneNumberRegex) { m ->
                        "<a href=\"tel:${m.value}\">${m.value}</a>"
                    }
                }
                if (matchUrl) {
                    replaced = replaced.replace(urlRegex) { m ->
                        "<a href=\"${m.value}\">${m.value}</a>"
                    }
                }
                replaced
            }

        safeBuilder.append(processedTail)
    }

    var lastPos = 0
    val enrichedText = safeBuilder.toString()

    anchorRegex.findAll(enrichedText).forEach { match ->
        val range = match.range
        val href = match.groupValues[1]
        val label = match.groupValues[2]


        // Add text before link
        if (lastPos < range.first) {
            append(enrichedText.substring(lastPos, range.first))
        }

        // Add the link
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "ACTION",
                styles = linkStyles,
                linkInteractionListener = {
                    onLinkClicked(href)
                }
            )
        ) {
            append(label)
        }

        lastPos = range.last + 1
    }

    // Add the trailing text after the last link
    if (lastPos < enrichedText.length) {
        append(enrichedText.substring(lastPos))
    }
}

/** Builds text with a single link represented by a text */
@Composable
fun buildAnnotatedLink(
    text: String,
    linkTexts: List<String>,
    onLinkClicked: (link: String, index: Int) -> Unit
) = buildAnnotatedString {
    append(text.substring(
        startIndex = 0,
        endIndex = linkTexts.firstOrNull()?.let { text.indexOf(it) } ?: text.length
    ))
    linkTexts.forEachIndexed { index, linkTextWithin ->
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "ACTION",
                styles = LocalTheme.current.styles.link,
                linkInteractionListener = {
                    onLinkClicked(linkTextWithin, index)
                },
            ),
        ) {
            append(linkTextWithin)
        }
        // space between this and next link
        if(linkTexts.size > 1 && index != linkTexts.lastIndex) {
            append(
                text.substring(
                    startIndex = text.indexOf(linkTextWithin) + linkTextWithin.length,
                    endIndex = linkTexts.getOrNull(index + 1)?.let { text.indexOf(it) } ?: text.length
                )
            )
        }
    }
    if(linkTexts.isNotEmpty()) {
        append(text.substring(
            startIndex = text.indexOf(linkTexts.last()) + linkTexts.last().length,
            endIndex = text.length
        ))
    }
}
