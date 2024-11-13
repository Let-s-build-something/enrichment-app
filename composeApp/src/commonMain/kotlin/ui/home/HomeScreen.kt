package ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_home
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.navigation.NavIconType
import components.UserProfileImage
import components.pull_refresh.RefreshableScreen
import data.io.user.NetworkItemIO
import data.io.user.PublicUserProfileIO
import future_shared_module.ext.scalingClickable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.home.DemoData.acquaintances
import ui.home.DemoData.community
import ui.home.DemoData.family
import ui.home.DemoData.friends
import ui.home.DemoData.strangers
import ui.network.list.NetworkListRepository.Companion.demoData
import ui.network.profile.UserProfileLauncher

/**
 * Screen for the home page
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val selectedProfile = remember {
        mutableStateOf<PublicUserProfileIO?>(null)
    }
    val items = remember {
        mutableStateListOf<NetworkItemIO>()
    }

    if(selectedProfile.value != null) {
        UserProfileLauncher(
            userProfile = selectedProfile.value,
            onDismissRequest = {
                selectedProfile.value = null
            }
        )
    }

    RefreshableScreen(
        title = stringResource(Res.string.screen_home),
        navIconType = NavIconType.HOME,
        viewModel = viewModel
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxHeight()) {
                repeat(5) { index ->
                    val isSelected = remember(index) { 
                        mutableStateOf(index == 0)
                    }
                    
                    LaunchedEffect(isSelected.value) {
                        val data = when(index) {
                            0 -> family
                            1 -> friends
                            2 -> acquaintances
                            3 -> community
                            else -> strangers
                        }

                        if(isSelected.value) {
                            items.addAll(data)
                        }else {
                            items.removeAll(data)
                        }
                    }
                    
                    Text(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .width(80.dp)
                            .background(color = when(index) {
                                0 -> SharedColors.GREEN_CORRECT
                                1 -> LocalTheme.current.colors.brandMain
                                2 -> SharedColors.YELLOW
                                3 -> SharedColors.ORANGE
                                else -> SharedColors.RED_ERROR
                            })
                            .scalingClickable {
                                isSelected.value = !isSelected.value
                            },
                        text = when(index) {
                            0 -> "Family"
                            1 -> "Friends"
                            2 -> "Acquaintances"
                            3 -> "Community"
                            else -> "Strangers"
                        },
                        style = LocalTheme.current.styles.category.copy(Color.White)
                    )
                }
            }
            Column(modifier = Modifier.fillMaxHeight()) {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = GridCells.Fixed(2)
                ) {
                    items(3) { index ->
                        demoData.getOrNull(index)?.let { user ->
                            Box(modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = SharedColors.RED_ERROR,
                                            shape = CircleShape
                                        )
                                        .size(24.dp)
                                        .padding(4.dp)
                                        .zIndex(2f)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = listOf(1, 2, 3, 4).random().toString(),
                                        style = LocalTheme.current.styles.category.copy(
                                            color = Color.White
                                        )
                                    )
                                }
                                UserProfileImage(
                                    modifier = Modifier
                                        .requiredSize(80.dp)
                                        .scalingClickable {
                                            // open chat
                                        },
                                    model = user.photoUrl,
                                    tag = user.tag
                                )
                            }
                        }
                    }
                    items(
                        items
                            .sortedByDescending {
                                it.proximity
                            }
                    ) { user ->
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .background(
                                    color = when(user.proximity?.toInt()) {
                                        10 -> SharedColors.GREEN_CORRECT
                                        9 -> LocalTheme.current.colors.brandMain
                                        in 4..8 -> SharedColors.YELLOW
                                        3 -> SharedColors.ORANGE
                                        else -> SharedColors.RED_ERROR
                                    }
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserProfileImage(
                                modifier = Modifier
                                    .requiredSize(80.dp)
                                    .scalingClickable {
                                        // open chat
                                    },
                                model = user.photoUrl,
                                tag = user.tag
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = user.displayName ?: "",
                                    style = LocalTheme.current.styles.category
                                )
                                Text(
                                    text = "Last sent message between you and me",
                                    style = LocalTheme.current.styles.regular
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private object DemoData {
    val family = listOf(
        NetworkItemIO(proximity = 10.1f, displayName = "Dad", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6"),
        NetworkItemIO(proximity = 10.7f, displayName = "Mom", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6d"),
        NetworkItemIO(proximity = 10.9f, displayName = "Sister", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098dc6d"),
        NetworkItemIO(proximity = 10.4f, displayName = "Brother", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098db6d"),
        NetworkItemIO(proximity = 10.9f, displayName = "Son", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6ed"),
        NetworkItemIO(proximity = 10.2f, displayName = "Grandma", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098dg6d"),
        NetworkItemIO(proximity = 10.1f, displayName = "Grandpa", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6sd")
    )

    val friends = listOf(
        NetworkItemIO(proximity = 9.9f, displayName = "Jack", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6"),
        NetworkItemIO(proximity = 9.3f, displayName = "Peter", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6dl"),
        NetworkItemIO(proximity = 9.2f, displayName = "James", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "20l98dc6d"),
        NetworkItemIO(proximity = 9.6f, displayName = "Mark", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098dbl6d"),
        NetworkItemIO(proximity = 9.8f, displayName = "Carl", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "209l8d6ed"),
        NetworkItemIO(proximity = 9.1f, displayName = "Arnold", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098ldg6d"),
    )

    val acquaintances = listOf(
        NetworkItemIO(proximity = 8.5f, displayName = "Jack", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6"),
        NetworkItemIO(proximity = 8.3f, displayName = "Peter", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098d6dl"),
        NetworkItemIO(proximity = 8.77f, displayName = "James", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "20l98dc6d"),
        NetworkItemIO(proximity = 8.7f, displayName = "Mark", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098dbl6d"),
        NetworkItemIO(proximity = 8.8f, displayName = "Carl", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "209l8d6ed"),
        NetworkItemIO(proximity = 8.2f, displayName = "Arnold", photoUrl = "https://picsum.photos/100", tag = "2098d6", publicId = "2098ldg6d"),
    )

    val community = demoData.map { it.copy(proximity = (40..70).random().div(10f)) }
    val strangers = demoData
}
