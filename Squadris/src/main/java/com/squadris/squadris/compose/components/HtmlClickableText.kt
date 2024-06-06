package com.squadris.squadris.compose.components

import android.graphics.Typeface
import android.text.Html
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.squadris.squadris.compose.theme.LocalTheme

private const val LINK_TAG = "LINK_TAG"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HtmlClickableText(
    modifier: Modifier = Modifier,
    text: String,
    linkStyle: TextStyle = LocalTheme.current.styles.linkText,
    style: TextStyle = LocalTheme.current.styles.category,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE,
    onLinkHover: (uri: String) -> Unit = {},
    onLinkClick: (uri: String) -> Unit = {},
    onHover: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val spanned = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)

    val annotatedText = remember(text) {
        buildAnnotatedString {
            append(spanned.toString())

            spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val startIndex = spanned.getSpanStart(span)
                val endIndex = spanned.getSpanEnd(span)

                when(span) {
                    is URLSpan -> {
                        addStyle(style = linkStyle.toSpanStyle(), start = startIndex, end = endIndex)
                        addStringAnnotation(tag = LINK_TAG, annotation = span.url, start = startIndex, end = endIndex)
                    }
                    is StyleSpan -> {
                        span.toSpanStyle()?.let { addStyle(style = it, start = startIndex, end = endIndex) }
                    }
                    is UnderlineSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start = startIndex, end = endIndex)
                    }
                    is StrikethroughSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start = startIndex, end = endIndex)
                    }
                }
            }
        }
    }


    ClickableText(
        modifier = modifier,
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        text = annotatedText,
        onHover = { index ->
            index?.let {
                val annotationItem = annotatedText.getStringAnnotations(
                    tag = LINK_TAG,
                    start = it,
                    end = it
                ).firstOrNull()?.item

                if(annotationItem != null) {
                    onLinkHover(annotationItem)
                }else {
                    onHover()
                }
            }
        },
        onClick = {
            val annotationItem = annotatedText.getStringAnnotations(
                tag = LINK_TAG,
                start = it,
                end = it
            ).firstOrNull()?.item

            if(annotationItem != null) {
                onLinkClick(annotationItem)
            }else {
                onClick()
            }
        }
    )
}

fun StyleSpan.toSpanStyle(): SpanStyle? {
    return when(style) {
        Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        else -> null
    }
}