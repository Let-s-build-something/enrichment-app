package ui.conversation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_message_audio
import augmy.composeapp.generated.resources.accessibility_message_file
import augmy.composeapp.generated.resources.accessibility_message_pdf
import augmy.composeapp.generated.resources.accessibility_message_presentation
import augmy.composeapp.generated.resources.accessibility_message_text
import augmy.composeapp.generated.resources.logo_pdf
import augmy.composeapp.generated.resources.logo_powerpoint
import augmy.interactive.shared.ui.theme.LocalTheme
import base.utils.MediaType
import base.utils.PlatformFileShell
import base.utils.getMediaType
import coil3.toUri
import components.AsyncSvgImage
import components.PlatformFileImage
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.conversation.components.gif.GifImage

/**
 * Media element, which is anything from an image, video, gif, to any type of file
 * @param url full url path to the media
 * @param media reference to a local file containing the media
 * @param contentDescription textual description of the content
 */
@Composable
fun MediaElement(
    modifier: Modifier = Modifier,
    url: String? = null,
    media: PlatformFile? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Inside
) {
    if(!url.isNullOrBlank() || media != null) {
        when(val mediaType = getMediaType(
            (url?.toUri()?.path ?: url)?.substringAfterLast(".") ?: media?.extension ?: ""
        )) {
            MediaType.IMAGE -> {
                if (media != null) {
                    PlatformFileImage(
                        modifier = modifier.wrapContentWidth(),
                        contentScale = contentScale,
                        media = media
                    )
                } else if(url != null) {
                    AsyncSvgImage(
                        modifier = modifier.wrapContentWidth(),
                        model = url,
                        contentScale = contentScale,
                        contentDescription = contentDescription
                    )
                }
            }
            MediaType.GIF -> {
                @Suppress("IMPLICIT_CAST_TO_ANY")
                (if(media != null) PlatformFileShell(media) else url)?.let { data ->
                    GifImage(
                        modifier = modifier
                            .zIndex(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .wrapContentWidth(),
                        data = data,
                        contentDescription = contentDescription,
                        contentScale = contentScale
                    )
                }
            }
            MediaType.VIDEO -> {
                // TODO local video
            }
            else -> {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconModifier = modifier.sizeIn(maxHeight = 50.dp, maxWidth = 50.dp)
                    when(mediaType) {
                        MediaType.PDF -> {
                            Image(
                                modifier = iconModifier,
                                painter = painterResource(Res.drawable.logo_pdf),
                                contentDescription = stringResource(Res.string.accessibility_message_pdf)
                            )
                        }
                        MediaType.AUDIO -> {
                            Icon(
                                modifier = iconModifier,
                                imageVector = Icons.Outlined.GraphicEq,
                                tint = LocalTheme.current.colors.secondary,
                                contentDescription = stringResource(Res.string.accessibility_message_audio)
                            )
                        }
                        MediaType.TEXT -> {
                            Icon(
                                modifier = iconModifier,
                                imageVector = Icons.Outlined.Description,
                                tint = LocalTheme.current.colors.secondary,
                                contentDescription = stringResource(Res.string.accessibility_message_text)
                            )
                        }
                        MediaType.PRESENTATION -> {
                            Image(
                                modifier = iconModifier,
                                painter = painterResource(Res.drawable.logo_powerpoint),
                                contentDescription = stringResource(Res.string.accessibility_message_presentation)
                            )
                        }
                        else -> {
                            Icon(
                                modifier = iconModifier,
                                imageVector = Icons.Outlined.FilePresent,
                                tint = LocalTheme.current.colors.secondary,
                                contentDescription = stringResource(Res.string.accessibility_message_file)
                            )
                        }
                    }
                }
            }
        }
    }
}