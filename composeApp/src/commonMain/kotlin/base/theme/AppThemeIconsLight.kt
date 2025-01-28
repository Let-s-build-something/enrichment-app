package base.theme

import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.logo_apple_dark
import augmy.composeapp.generated.resources.logo_giphy_light
import augmy.composeapp.generated.resources.logo_google_light
import augmy.composeapp.generated.resources.logo_matrix_light
import augmy.interactive.shared.ui.theme.ThemeIcons
import org.jetbrains.compose.resources.DrawableResource

/** icons specific to main app theme LocalTheme.current */
object AppThemeIconsLight: ThemeIcons {
    override val googleSignUp: DrawableResource = Res.drawable.logo_google_light
    override val appleSignUp: DrawableResource = Res.drawable.logo_apple_dark
    override val matrixSignUp: DrawableResource = Res.drawable.logo_matrix_light
    override val giphy: DrawableResource = Res.drawable.logo_giphy_light
}