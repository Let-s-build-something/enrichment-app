package ui.network.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_inclusion_proximity_title
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme
import components.UserProfileImage
import data.NetworkProximityCategory
import data.io.user.NetworkItemIO
import org.jetbrains.compose.resources.stringResource
import ui.network.add_new.NetworkAddNewModel

@Composable
fun ProximityPicker(
    modifier: Modifier = Modifier,
    newItem: NetworkItemIO,
    model: NetworkAddNewModel,
    selectedCategory: MutableState<NetworkProximityCategory>
) {
    val customColors = model.customColors.collectAsState(initial = hashMapOf())
    val recommendedUsers = model.recommendedUsers.collectAsState(initial = hashMapOf())

    LaunchedEffect(Unit) {
        model.requestRecommendedUsers(newItem.publicId)
    }

    Column(modifier = modifier) {
        Text(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            text = stringResource(Res.string.network_inclusion_proximity_title),
            style = LocalTheme.current.styles.subheading
        )
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(top = 4.dp)
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            val corner = LocalTheme.current.shapes.componentCornerRadius
            NetworkProximityCategory.entries.forEachIndexed { index, category ->
                val weight = animateFloatAsState(
                    targetValue = if(selectedCategory.value == category) 3f else 1f,
                    label = "weightChange"
                )
                val colorAlpha = animateFloatAsState(
                    targetValue = if(selectedCategory.value == category) 1f else .7f,
                    label = "alphaChange"
                )

                Column(
                    modifier = Modifier
                        .scalingClickable(
                            scaleInto = .95f,
                            hoverEnabled = false
                        ) {
                            selectedCategory.value = category
                        }
                        .background(
                            color = (customColors.value[category] ?: category.color).copy(colorAlpha.value),
                            shape = RoundedCornerShape(
                                bottomStart = if(index == 0) corner else 0.dp,
                                topStart= if(index == 0) corner else 0.dp,
                                bottomEnd = if(index == NetworkProximityCategory.entries.lastIndex) corner else 0.dp,
                                topEnd = if(index == NetworkProximityCategory.entries.lastIndex) corner else 0.dp
                            )
                        )
                        .fillMaxHeight()
                        .weight(weight.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        text = stringResource(category.res),
                        style = LocalTheme.current.styles.category.copy(
                            color = Color.White.copy(colorAlpha.value),
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1
                    )
                    recommendedUsers.value?.get(category).orEmpty()
                        .toMutableList()
                        .apply {
                            if(selectedCategory.value == category) {
                                add(
                                    index = this.size.div(2)
                                        .coerceAtMost(this.lastIndex)
                                        .coerceAtLeast(0),
                                    element = newItem
                                )
                            }
                        }
                        .forEach { user ->
                            Row(
                                modifier = Modifier
                                    .animateContentSize()
                                    .padding(LocalTheme.current.shapes.betweenItemsSpace / 2),
                                horizontalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserProfileImage(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(42.dp),
                                    media = user.avatar,
                                    name = user.displayName,
                                    tag = user.tag
                                )
                                if(selectedCategory.value == category) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = user.displayName ?: "",
                                        style = LocalTheme.current.styles.category,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}
