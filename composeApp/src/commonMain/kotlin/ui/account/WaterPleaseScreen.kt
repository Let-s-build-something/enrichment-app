package ui.account

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.screen_water_please
import augmy.composeapp.generated.resources.work_in_progress
import augmy.interactive.shared.ui.theme.LocalTheme
import base.BrandBaseScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun WaterPleaseScreen() {
    BrandBaseScreen(
        title = stringResource(Res.string.screen_water_please)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(Res.string.work_in_progress),
            style = LocalTheme.current.styles.subheading
        )
    }
}