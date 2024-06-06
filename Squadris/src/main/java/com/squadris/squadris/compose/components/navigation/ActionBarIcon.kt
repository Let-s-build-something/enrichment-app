package com.squadris.squadris.compose.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.squadris.squadris.compose.components.AutoResizeText
import com.squadris.squadris.compose.components.FontSizeRange
import com.squadris.squadris.compose.theme.LocalTheme

@Composable
fun ActionBarIcon(
    modifier: Modifier = Modifier,
    text: String,
    imageVector: ImageVector? = null,
    imageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .heightIn(max = 64.0.dp)
            .then(modifier)
            .widthIn(min = 24.dp, max = 100.dp)
            .clip(LocalTheme.current.shapes.rectangularActionShape)
            .clickable(
                indication = rememberRipple(
                    bounded = true,
                    color = LocalTheme.current.colors.contrastActionLight
                ),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick.invoke()
            }.padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            imageUrl != null -> {
                AsyncImage(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(24.dp),
                    model = imageUrl,
                    contentDescription = text
                )
            }
            imageVector != null -> {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = imageVector,
                    contentDescription = text,
                    tint = LocalTheme.current.colors.tetrial
                )
            }
        }
        AutoResizeText(
            modifier = Modifier
                .wrapContentHeight()
                .padding(top = 2.dp),
            text = text,
            color = LocalTheme.current.colors.tetrial,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            // some users tune up font size so high we can draw it otherwise
            fontSizeRange = FontSizeRange(
                min = 9.5.sp,
                max = 14.sp
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
    ActionBarIcon(
        text = "test action",
        imageVector = Icons.Outlined.Dashboard
    )
}