package base.global

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import base.global.verification.DeviceVerificationLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationPopUps() {
    DeviceVerificationLauncher()
}