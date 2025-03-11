package base.global

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.hint_unauthorized_matrix_action
import augmy.composeapp.generated.resources.hint_unauthorized_matrix_text
import augmy.composeapp.generated.resources.hint_unauthorized_matrix_title
import augmy.composeapp.generated.resources.no_connection_description
import augmy.composeapp.generated.resources.no_connection_title
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.LocalDeviceType
import augmy.interactive.shared.ui.theme.LocalTheme
import base.theme.Colors
import components.notification.InfoHintBox
import data.io.user.UserIO
import data.shared.SharedModel
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ui.account.profile.DisplayNameChangeLauncher

@Composable
fun ColumnScope.InformationLines(
    sharedModel: SharedModel = koinViewModel(),
    currentUser: UserIO?
) {
    val networkConnectivity = sharedModel.networkConnectivity.collectAsState()


    // no stable internet connection
    AnimatedVisibility(networkConnectivity.value?.isStable == false) {
        NoConnectionLine()
    }

    // missing display name
    AnimatedVisibility(
        currentUser != null
                && currentUser.displayName == null
                && !sharedModel.awaitingAutologin
    ) {
        MissingDisplayNameLine()
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun NoConnectionLine() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            Res.readBytes("files/no_connection.lottie")
        )
    }

    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.height(90.dp),
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = Int.MAX_VALUE,
                    reverseOnRepeat = true
                ),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.no_connection_title),
                    style = LocalTheme.current.styles.title.copy(color = Color.White)
                )
                Text(
                    text = stringResource(Res.string.no_connection_description),
                    style = LocalTheme.current.styles.regular.copy(color = Colors.GrayLight)
                )
            }
            if(LocalDeviceType.current != WindowWidthSizeClass.Compact) {
                Spacer(Modifier.width(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingDisplayNameLine() {
    val density = LocalDensity.current
    val showNameChangeLauncher = remember { mutableStateOf(false) }

    if(showNameChangeLauncher.value) {
        DisplayNameChangeLauncher {
            showNameChangeLauncher.value = false
        }
    }

    InfoHintBox(
        modifier = Modifier
            .scalingClickable(scaleInto = .95f) {
                showNameChangeLauncher.value = true
            }
            .fillMaxWidth(),
        icon = {
            Text(
                text = stringResource(Res.string.hint_unauthorized_matrix_action),
                style = LocalTheme.current.styles.title.copy(
                    color = Colors.ProximityContacts,
                    fontSize = with(density) { 36.dp.toSp() }
                )
            )
        },
        title = stringResource(Res.string.hint_unauthorized_matrix_title),
        text = stringResource(Res.string.hint_unauthorized_matrix_text)
    )
}