package ui.conversation.components.message

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import data.sensor.SensorEvent
import data.sensor.SensorEventListener
import data.sensor.registerGravityListener


//TODO let's just do a single indicator for now and then move from there
fun calculateIndicationOffset(size: Size, gx: Float, gy: Float, gz: Float): Pair<Float, Float> {
    // bottom left... z = 9.81, x = 0, y = 0
    // bottom right... z = 9.81, x = 0, y = 0
    // bottom middle... z in 0..9.81, x in 5..9.81, y in 0..5
    // top left... z = -9.81, x = 0, y = 0
    // top right... z = -9.81, x = 0, y = 0
    // top middle... z in 0..-9.81, , x in 5..9.81, y in 0..5
    // left middle... z in 0..9.81, x in 0..-9.81, y in -5..5
    // right middle... z in 0..9.81, x in 0..9.81, y in -5..5

    return 0f to 0f
}


@Composable
fun GravityIndicationContainer(
    modifier: Modifier = Modifier,
    indicationColor: Color = LocalTheme.current.colors.tetrial,
    backgroundColor: Color,
    shape: Shape,
    content: @Composable BoxScope.() -> Unit= {}
) {
    val gravityValues = remember {
        mutableStateOf(Triple(0f, 0f, 0f))
    }

    val customBrush = rememberUpdatedState(
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                return LinearGradientShader(
                    colors = listOf(
                        backgroundColor,
                        indicationColor,
                        backgroundColor,
                        backgroundColor,
                        indicationColor,
                        backgroundColor,
                    ),
                    colorStops = listOf(
                        0.225f, 0.25f, 0.275f,
                        0.725f, 0.75f, 0.775f
                    ),
                    tileMode = TileMode.Decal,
                    from = Offset(0f, 0f),
                    to = Offset(size.width, size.height)
                )
            }
        }
    )

    LaunchedEffect(Unit) {
        registerGravityListener(
            object: SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.let { values ->
                        gravityValues.value = Triple(values[0], values[1], values[2])
                    }
                }
                override fun onAccuracyChanged(accuracy: Int) {

                }
            }
        )
    }

    Box(
        modifier = modifier
            .border(
                width = 4.dp,
                shape = shape,
                brush = customBrush.value
            ),
        content = content
    )
}
