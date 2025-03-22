package ui.conversation.components.experimental.gravity

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.conversation.components.experimental.gravity.GravityUseCase.Companion.FULL_GRAVITY
import ui.conversation.components.experimental.gravity.GravityUseCase.Companion.MAX_FRACTION_OFFSET
import kotlin.math.absoluteValue

private data class GravityIndicationStops(
    val stops: List<Float>,
    val from: Offset,
    val to: Offset,
)

private fun calculateIndicationStops(
    size: Size,
    gx: Float,
    gy: Float,
    gz: Float,
    counterValue: Float? = null,
    counterOffset: Float? = null
): GravityIndicationStops {
    val verticalFraction = ((gy.absoluteValue / FULL_GRAVITY).minus(1f).absoluteValue * MAX_FRACTION_OFFSET).let {
        if(counterOffset != null) {
            it / 2 + counterOffset / 2
        }else it / 2
    }
    val value = ((gx / FULL_GRAVITY / 2 + .5f).minus(1).absoluteValue.coerceAtMost(1f)).let {
        if(counterValue != null) {
            it + counterValue / 2
        }else it
    }
    //val depthFraction = gz / FULL_GRAVITY * 2

    val stops = listOf(
        value - verticalFraction,
        value,
        value + verticalFraction,
    )
    //val horizontalOffset = size.width * (1f - value) * 0.8f
    //val verticalOffset = size.height * (0.5f + depthFraction)

    val from = Offset.Zero //Offset(horizontalOffset, verticalOffset)
    val to = Offset(size.width, size.height) //Offset(size.width - horizontalOffset, size.height - verticalOffset)

    return GravityIndicationStops(
        stops = stops,
        from = from,
        to = to
    )
}

@Composable
fun GravityIndicationContainer(
    modifier: Modifier = Modifier,
    indicationColor: Color = LocalTheme.current.colors.tetrial,
    backgroundColor: Color,
    shape: Shape,
    gravityData: GravityData?,
    content: @Composable BoxScope.() -> Unit= {}
) {
    val index = if(gravityData != null) {
        rememberSaveable(gravityData) {
            mutableStateOf(0)
        }
    }else null

    if(!gravityData?.values.isNullOrEmpty() && index?.value != gravityData?.values?.size) {
        val scope = rememberCoroutineScope()
        val offset = remember(gravityData) {
            Animatable(gravityData?.values?.firstOrNull()?.offset ?: 0f)
        }
        val fraction = remember(gravityData) {
            Animatable(gravityData?.values?.firstOrNull()?.fraction ?: 0f)
        }

        LaunchedEffect(Unit) {
            scope.coroutineContext.cancelChildren()
            scope.launch {
                /*registerGravityListener(
                    listener = object: SensorEventListener {
                        override lateinit var instance: Any
                        override var isInitialized: Boolean = false

                        override fun onSensorChanged(event: SensorEvent?) {
                            event?.values?.let {
                                gravityValues.value = Triple(it[0], it[1], it[2])
                            }
                        }
                        override fun onAccuracyChanged(accuracy: Int) {}
                    },
                    sensorDelay = SensorDelay.Normal
                )*/

                while(index?.value != null && index.value < (gravityData?.values?.size ?: 0)) {
                    gravityData?.tickMs?.let { delay(it) }
                    gravityData?.values?.getOrNull(index.value)?.let {
                        fraction.animateTo(it.fraction)
                        offset.animateTo(it.offset)
                    }
                    index.value += 1
                }
                index?.value = gravityData?.values?.size ?: 0
            }
        }

        val customBrush = rememberUpdatedState(
            fraction.value.let { counterValue ->
                object: ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        return LinearGradientShader(
                            colors = listOf(
                                backgroundColor,
                                indicationColor,
                                backgroundColor,
                            ),
                            colorStops = listOf(
                                counterValue - offset.value,
                                counterValue,
                                counterValue + offset.value,
                            ),
                            tileMode = TileMode.Decal,
                            from = Offset.Zero,
                            to = Offset(size.width, size.height / 2)
                        )
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    shape = shape,
                    brush = customBrush.value
                ).then(modifier)
        ) {
            content()
        }
    }else {
        Box(modifier = modifier, content = content)
    }
}
