package lets.build.chatenrichment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import com.squadris.squadris.compose.theme.LocalTheme
import dagger.hilt.android.AndroidEntryPoint
import lets.build.chatenrichment.ui.NavigationComponent
import lets.build.chatenrichment.ui.theme.ChatEnrichmentTheme

/** Main single activity for interaction with the application and hosting the navigation */
@AndroidEntryPoint
class MainActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            ChatEnrichmentTheme {
                Box(modifier = Modifier.background(color = LocalTheme.current.colors.brandMain)) {
                    NavigationComponent(activity = this@MainActivity)
                }
            }
        }
    }
}