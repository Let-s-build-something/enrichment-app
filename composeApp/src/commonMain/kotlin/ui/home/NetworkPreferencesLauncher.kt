package ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.button_confirm
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.network_preferences_circle_colors
import augmy.composeapp.generated.resources.network_preferences_default_message
import augmy.composeapp.generated.resources.network_preferences_default_name
import augmy.composeapp.generated.resources.network_preferences_title
import augmy.interactive.shared.ui.components.MinimalisticIcon
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.theme.Colors
import components.network.NetworkItemRow
import data.NetworkProximityCategory
import data.io.user.NetworkItemIO
import augmy.interactive.shared.ext.scalingClickable
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet for tuning network configurations and preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPreferencesLauncher(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit
) {
    val customizedColors = viewModel.customColors.collectAsState(initial = mapOf())
    val categoryInEdit = remember {
        mutableStateOf<NetworkProximityCategory?>(null)
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 16.dp
        ),
        sheetState = sheetState,
        dragHandle = {},
        onDismissRequest = onDismissRequest
    ) {
        Text(
            text = stringResource(Res.string.network_preferences_title),
            style = LocalTheme.current.styles.subheading
        )

        Text(
            modifier = Modifier.padding(
                top = LocalTheme.current.shapes.betweenItemsSpace,
                start = 8.dp
            ),
            text = stringResource(Res.string.network_preferences_circle_colors),
            style = LocalTheme.current.styles.category
        )
        NetworkProximityCategory.entries.forEach { category ->
            val defaultColor = customizedColors.value[category] ?: category.color

            Column(modifier = Modifier.padding(start = 16.dp)) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .scalingClickable {
                            categoryInEdit.value = if(categoryInEdit.value == category) null else category
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorTile(color = defaultColor)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(category.res),
                        style = LocalTheme.current.styles.category
                    )
                }
                AnimatedVisibility(category == categoryInEdit.value) {
                    val selectedColor = remember {
                        mutableStateOf(defaultColor)
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        NetworkItemRow(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            data = NetworkItemIO(
                                name = stringResource(Res.string.network_preferences_default_name),
                                lastMessage = stringResource(Res.string.network_preferences_default_message),
                                tag = "E7D37F",
                                photoUrl = "https://augmy.org/storage/img/imjustafish.jpg"
                            ),
                            color = selectedColor.value
                        )
                        LazyRow(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace)
                        ) {
                            items(items = listOf(category.color) + colors.minus(category.color)) { color ->
                                ColorTile(
                                    color = color,
                                    isSelected = selectedColor.value == color,
                                    onClick = {
                                        selectedColor.value = color
                                    }
                                )
                            }
                        }
                        AnimatedVisibility(selectedColor.value != defaultColor) {
                            Row(
                                modifier = Modifier.padding(end = 16.dp, top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MinimalisticIcon(
                                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                                    contentDescription = stringResource(Res.string.button_dismiss),
                                    tint = SharedColors.RED_ERROR,
                                    onTap = {
                                        selectedColor.value = defaultColor
                                    }
                                )
                                MinimalisticIcon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = stringResource(Res.string.button_confirm),
                                    tint = LocalTheme.current.colors.brandMain,
                                    onTap = {
                                        viewModel.updateColorPreference(
                                            category = category,
                                            color = selectedColor.value
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private val colors = listOf(
    Colors.ProximityFamily,
    Colors.ProximityPeers,
    Colors.ProximityCommunity,
    Colors.ProximityContacts,
    Colors.ProximityPublic,
    Colors.AtomicTangerine,
    SharedColors.RED_ERROR,
    SharedColors.ORANGE,
    SharedColors.YELLOW,
    Color(0xFFC68E6F), // Peers alternative
    Color(0xFFD6C598), // Community alternative
)

/** Tile displaying selectable color */
@Composable
fun ColorTile(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    color: Color,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(if(isSelected) 48.dp else 32.dp)
            .background(
                color = color,
                shape = LocalTheme.current.shapes.rectangularActionShape
            )
            .animateContentSize()
            .scalingClickable {
                onClick()
            }
    )
}
