package lets.build.chatenrichment.ui.base

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.squadris.squadris.compose.base.BaseScreen
import com.squadris.squadris.compose.base.LocalNavController
import com.squadris.squadris.compose.components.collapsing_layout.CollapsingLayoutState
import com.squadris.squadris.compose.components.collapsing_layout.rememberCollapsingLayout
import com.squadris.squadris.compose.components.navigation.NavIconType
import com.squadris.squadris.compose.theme.LocalTheme
import lets.build.chatenrichment.navigation.NavigationComponent
import lets.build.chatenrichment.data.shared.SharedViewModel

/**
 * Most simple screen for implementing bussiness level logic
 */
@Composable
fun BrandBaseScreen(
    modifier: Modifier = Modifier,
    navIconType: NavIconType = NavIconType.BACK,
    title: String? = null,
    collapsingLayoutState: CollapsingLayoutState = rememberCollapsingLayout(),
    subtitle: String? = null,
    onBackPressed: () -> Boolean = { true },
    actionIcons: (@Composable RowScope.() -> Unit)? = null,
    onNavigationIconClick: (() -> Unit)? = null,
    contentModifier: Modifier = Modifier,
    appBarVisible: Boolean = true,
    containerColor: Color? = LocalTheme.current.colors.backgroundLight,
    contentColor: Color = Color.Transparent,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val navController = LocalNavController.current
    val sharedViewModel: SharedViewModel = hiltViewModel<SharedViewModel>()

    val currentUser = sharedViewModel.currentUser.collectAsState()

    val navIconClick: (() -> Unit)? = when {
        navIconType == NavIconType.HOME -> {
            {
                navController?.popBackStack(NavigationComponent.Home, inclusive = false)
            }
        }
        onNavigationIconClick != null -> onNavigationIconClick
        else -> null
    }

    BaseScreen(
        modifier = modifier,
        title = title,
        collapsingLayoutState = collapsingLayoutState,
        subtitle = subtitle,
        onBackPressed = onBackPressed,
        contentModifier = contentModifier,
        actionIcons = actionIcons ?: {
            /*DefaultAppBarActions(
                isUserSignedIn = currentUser.value != null,
                userPhotoUrl = currentUser.value?.photoUrl?.toString()
            )*/
        },
        appBarVisible = appBarVisible,
        containerColor = containerColor,
        contentColor = contentColor,
        onNavigationIconClick = navIconClick,
        floatingActionButtonPosition = floatingActionButtonPosition,
        floatingActionButton = floatingActionButton,
        navigationIcon = navIconType.imageVector,
        content = content
    )
}