package base.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.theme.LocalTheme
import korlibs.io.util.substringBeforeOrNull
import org.jetbrains.compose.resources.stringResource

/** Horizontal bar mainly for nested navigation. This can be seen on tablet and larger devices. */
@Composable
fun NestedNavigationBar(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val previousDestination = remember {
        mutableStateOf<NavigationNode?>(null)
    }
    val listener = remember {
        NavController.OnDestinationChangedListener { controller, _, _ ->
            val route = controller.previousBackStackEntry?.destination?.route

            previousDestination.value = NavigationNode.allNodes.find { node ->
                (route?.substringBeforeOrNull("?") ?: route) == node.route
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        navController.addOnDestinationChangedListener(listener)

        onPauseOrDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = previousDestination.value != null
    ) {
        Row(
            modifier = modifier.scalingClickable(hoverEnabled = false) {
                navController.navigateUp()
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                tint = LocalTheme.current.colors.secondary,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = previousDestination.value?.titleRes?.let {
                    stringResource(it)
                } ?: "",
                style = LocalTheme.current.styles.regular
            )
        }
    }
}
