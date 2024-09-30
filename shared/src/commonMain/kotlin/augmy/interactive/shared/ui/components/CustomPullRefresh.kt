package augmy.interactive.shared.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import augmy.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val REFRESH_SIZE_MAX_DP = 150.0
private const val REFRESH_SIZE_MIN_DP = 100.0
const val REFRESH_RETURN_ANIMATION_LENGTH = 600

fun getDefaultPullRefreshSize(isSmallDevice: Boolean): Dp {
    return (if(isSmallDevice) REFRESH_SIZE_MIN_DP else REFRESH_SIZE_MAX_DP).dp
}

/** Gives random loading lottie animation */
private val loadingLottieAnims: List<String>
    get() = listOf(
        "loading_animation_3a3a3a.json",
        "loading_animation_45ddd2.json",
        "loading_animation_755334.json",
        "loading_animation_798c8e.json",
        "loading_animation_88fcac.json",
        "loading_animation_a6dfff.json",
        "loading_animation_a87b2a.json",
        "loading_animation_aa7d2e.json",
        "loading_animation_bcd8cb.json",
        "loading_animation_c0e0ed.json",
        "loading_animation_c0f3ff.json",
        "loading_animation_c19a2d.json",
        "loading_animation_c1a58e.json",
        "loading_animation_c5eae8.json",
        "loading_animation_cebb8e.json",
        "loading_animation_d5edef.json",
        "loading_animation_e2c888.json",
        "loading_animation_e51749.json",
        "loading_animation_e8e1cf.json",
        "loading_animation_efc6f7.json",
        "loading_animation_f27d2f.json",
        "loading_animation_f8ffb6.json",
        "loading_animation_fcc0de.json",
        "loading_animation_ff0000.json",
        "loading_animation_ffb6ae.json",
        "loading_animation_ffdbc0.json",
        "loading_animation_fff4c0.json",
    )

@OptIn(ExperimentalResourceApi::class)
suspend fun getRandomLoadingLottieAnim(): String {
    return Res.readBytes("files/${loadingLottieAnims.random()}").decodeToString()
}