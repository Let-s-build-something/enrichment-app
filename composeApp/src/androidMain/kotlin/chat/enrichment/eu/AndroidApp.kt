package chat.enrichment.eu

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import koin.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

class AndroidApp: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(applicationContext)
            androidLogger()
            modules(commonModule)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Box(modifier = Modifier.fillMaxSize().background(color = Color.White)) {
        Box(
            modifier = Modifier
                .size(width = 170.dp, height = 50.dp)
                .align(Alignment.Center)
                .border(
                    color = Color.Gray,
                    shape = CircleShape,
                    width = 2.dp
                )
                .background(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Red,
                        0.3f to Color.Green,
                        1.0f to Color. Blue,
                    ),
                    shape = EmotionalBackgroundShape(
                        salience = .5f,
                        frequency = .5f
                    )
                )
        )
    }
}

fun calculateRoundOutline(size: Size) {
    val smallerSide = size.width.coerceAtMost(size.height)
    val longerSide = size.width.coerceAtLeast(size.height)



    val roundedCorners = size.height * kotlin.math.PI
    val widthNoCorners = 2 * size.width - size.height/2
}

class EmotionalBackgroundShape(
    private val salience: Float,
    private val frequency: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = Outline.Generic(Path().apply {
        val averagePeriod = 9.dp
        val averageAmplitude = 20.dp
        val amplitude = with(density) {
            averageAmplitude.toPx() * salience
        }

        val radius = size.width / 2
        val outlineLength = 2 * radius * kotlin.math.PI
        val wavesCount = ceil(outlineLength / with(density) {
            averagePeriod.toPx()
        } * frequency).toInt()

        val wavyPath = Path().apply {
            val angleStep = (2 * Math.PI / wavesCount).toFloat()
            val centerX = size.width / 2
            val centerY = size.height / 2

            repeat(wavesCount) { i ->
                val angle = i * angleStep
                val nextAngle = (i + 1) * angleStep
                val direction = if (i % 2 == 0) amplitude else 0f
                val nextDirection = if (i % 2 == 0) 0f else amplitude

                val startX = centerX + (direction + radius) * cos(angle + angleStep / 2)
                val startY = centerY + (direction + radius) * sin(angle + angleStep / 2)

                val endX = centerX + (direction + radius) * cos(nextAngle + angleStep / 2)
                val endY = centerY + (direction + radius) * sin(nextAngle + angleStep / 2)

                val controlX = (startX + endX) / 2f + nextDirection * cos((i + 0.5f) * angleStep)
                val controlY = (startY + endY) / 2f + nextDirection * sin((i + 0.5f) * angleStep)


                if (i == 0) {
                    moveTo(startX, startY)
                    /*addOval(
                        Rect(
                            Offset(startX + 20f, startY + 20f),
                            Offset(startX - 20f, startY - 20f)
                        )
                    )*/
                }
                quadraticTo(
                    controlX,
                    controlY,
                    endX,
                    endY
                )
            }
            close()
        }
        addPath(wavyPath)
    })
}