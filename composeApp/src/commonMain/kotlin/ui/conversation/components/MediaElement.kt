package ui.conversation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import augmy.composeapp.generated.resources.accessibility_play
import augmy.composeapp.generated.resources.logo_pdf
import augmy.composeapp.generated.resources.logo_powerpoint
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import base.utils.MediaType
import base.utils.PlatformFileShell
import base.utils.getExtensionFromMimeType
import base.utils.getMediaType
import chaintech.videoplayer.model.PlayerConfig
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.ui.preview.VideoPreviewComposable
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import components.AsyncSvgImage
import components.PlatformFileImage
import data.io.social.network.conversation.message.MediaIO
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.extension
import korlibs.io.net.MimeType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.conversation.components.gif.GifImage

/**
 * Media element, which is anything from an image, video, gif, to any type of file
 * @param media reference to a remote file containing the media
 * @param localMedia reference to a local file containing the media
 * @param contentDescription textual description of the content
 */
@Composable
fun MediaElement(
    modifier: Modifier = Modifier,
    videoPlayerEnabled: Boolean = false,
    media: MediaIO? = null,
    localMedia: PlatformFile? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Inside,
    onTap: ((MediaType) -> Unit)? = null,
    enabled: Boolean = onTap != null,
    onLongPress: () -> Unit = {}
) {
    val mediaType = getMediaType(
        media?.mimetype ?: MimeType.getByExtension(localMedia?.extension ?: "").mime
    )
    val itemModifier = modifier.scalingClickable(
        enabled = enabled,
        scaleInto = .95f,
        hoverEnabled = false,
        onLongPress = {
            onLongPress()
        },
        onTap = {
            onTap?.invoke(mediaType)
        }
    )

    if(!media?.url.isNullOrBlank() || localMedia != null) {
        when(mediaType) {
            MediaType.IMAGE -> {
                if (localMedia != null) {
                    PlatformFileImage(
                        modifier = itemModifier.wrapContentWidth(),
                        contentScale = contentScale,
                        media = localMedia
                    )
                } else if(media?.url != null) {
                    AsyncSvgImage(
                        modifier = itemModifier.wrapContentWidth(),
                        model = media.path ?: media.url,
                        contentScale = contentScale,
                        contentDescription = contentDescription
                    )
                }
            }
            MediaType.GIF -> {
                @Suppress("IMPLICIT_CAST_TO_ANY")
                (if(localMedia != null) {
                    PlatformFileShell(localMedia)
                } else media?.path ?: media?.url)?.let { data ->
                    GifImage(
                        modifier = itemModifier
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
                if(videoPlayerEnabled) {
                    val theme = LocalTheme.current
                    val config = PlayerConfig(
                        isFullScreenEnabled = true,
                        isSpeedControlEnabled = false,
                        isMuteControlEnabled = false,
                        isSeekBarVisible = true,
                        seekBarThumbColor = theme.colors.brandMainDark,
                        seekBarActiveTrackColor = theme.colors.brandMain,
                        seekBarInactiveTrackColor = theme.colors.tetrial,
                        durationTextStyle = theme.styles.regular,
                        iconsTintColor = theme.colors.secondary,
                        loadingIndicatorColor = theme.colors.secondary,
                        isDurationVisible = true,
                        isFastForwardBackwardEnabled = false,
                        loop = false,
                        loaderView = {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .zIndex(1f)
                                    .requiredSize(32.dp),
                                color = LocalTheme.current.colors.brandMainDark,
                                trackColor = LocalTheme.current.colors.tetrial
                            )
                        },
                        videoFitMode = ScreenResize.FIT
                    )

                    VideoPlayerComposable(
                        modifier = itemModifier,
                        url = localMedia?.path ?: media?.path ?: media?.url ?: "",
                        playerConfig = config
                    )
                }else {
                    Box(
                        modifier = itemModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        if(enabled) {
                            Icon(
                                modifier = Modifier
                                    .zIndex(1f)
                                    .size(36.dp),
                                imageVector = Icons.Outlined.PlayArrow,
                                tint = Colors.GrayLight,
                                contentDescription = stringResource(Res.string.accessibility_play)
                            )
                        }

                        VideoPreviewComposable(
                            url = localMedia?.path ?: media?.path ?: media?.url ?: "",
                            loadingIndicatorColor = LocalTheme.current.colors.secondary,
                            frameCount = 1
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = itemModifier.width(IntrinsicSize.Min),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val iconModifier = modifier.fillMaxWidth(.3f).aspectRatio(1f)

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
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = "${localMedia?.name ?: media?.name ?: ""}.${localMedia?.extension ?: getExtensionFromMimeType(media?.mimetype)}",
                        style = LocalTheme.current.styles.regular
                    )
                }
            }
        }
    }
}