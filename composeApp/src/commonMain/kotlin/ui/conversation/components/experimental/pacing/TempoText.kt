package ui.conversation.components.experimental.pacing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.conversation.components.REGEX_GRAPHEME

/**
 * Visualization of person typing their message in the past.
 * It replicates the 1) tempo of typing 2) the stops, and 3) the emphasis
 */
@Composable
fun buildTempoString(
    key: Any? = null,
    text: AnnotatedString,
    style: TextStyle,
    enabled: Boolean,
    timings: List<Long>,
    onFinish: () -> Unit
): AnnotatedString {
    val aboveMedianIndexes = remember(key, timings) {
        mutableStateOf<MutableSet<Int>?>(null)
    }
    val graphemes = remember(text, key) {
        mutableStateOf<Sequence<MatchResult>?>(null)
    }
    text.spanStyles

    if(timings.isNotEmpty()) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                graphemes.value = REGEX_GRAPHEME.toRegex().findAll(text)

                val fourthQuartile = timings[timings.size / 4]
                val aboveMedianList = mutableSetOf<Int>()
                timings.forEachIndexed { index, timing ->
                    if(index < (graphemes.value?.count() ?: 0)
                        && timing > FULL_STOP_LENGTH / 2
                        && timing > fourthQuartile
                    ) {
                        aboveMedianList.add(
                            graphemes.value?.take(
                                // we skip possible space before the actual emphasis
                                if(graphemes.value?.elementAt(index)?.value.isNullOrBlank()
                                    && (timings.getOrNull(index + 1) ?: 0L) < FULL_STOP_LENGTH
                                ) {
                                    index + 1
                                } else index
                            )?.sumOf { it.value.length } ?: 0
                        )
                    }
                }
                aboveMedianIndexes.value = aboveMedianList
            }
        }
    }

    return when {
        timings.isEmpty() -> buildAnnotatedString {
            withStyle(style.toSpanStyle()) { append(text) }
        }
        !enabled -> {
            buildAnnotatedString {
                graphemes.value?.toList()?.forEachIndexed { index, matchResult ->
                    withStyle(
                        style.toSpanStyle().copy(
                            fontWeight = if(aboveMedianIndexes.value?.contains(index) == true) {
                                FontWeight.ExtraBold
                            }else null
                        )
                    ) {
                        append(matchResult.value)
                    }
                }
            }
        }
        else -> {
            val cancellableScope = rememberCoroutineScope()
            val scope = rememberCoroutineScope()

            var currentPosition by rememberSaveable(key, timings) {
                mutableStateOf(-1)
            }
            val isTicked = remember(key) {
                mutableStateOf(true)
            }

            LaunchedEffect(Unit, enabled) {
                scope.coroutineContext.cancelChildren()
                scope.launch {
                    delay(FLICKER_DELAY_LOOSE)
                    while (currentPosition <= timings.size) {
                        val timing = timings.getOrNull(currentPosition) ?: 1L
                        val index = currentPosition + 1

                        // if the index is 0, there may be a copy paste section we should skip
                        currentPosition = if(timings.getOrNull(index) == 0L) {
                            var final = index
                            for (i in index .. timings.size) {
                                if((timings.getOrNull(i) ?: 0L) == 0L) final++ else break
                            }
                            final
                        }else index

                        if(timing > 0) {
                            if(currentPosition < text.length) {
                                isTicked.value = true
                                cancellableScope.coroutineContext.cancelChildren()
                                cancellableScope.launch {
                                    delay(FULL_STOP_LENGTH)
                                    while(index < text.length) {
                                        isTicked.value = isTicked.value == false
                                        delay(400L)
                                    }
                                }
                            }
                            delay(timing)
                        }
                    }
                    onFinish()
                }
            }

            buildAnnotatedString {
                graphemes.value?.toList()?.forEachIndexed { index, matchResult ->
                    if(index == currentPosition && currentPosition < (graphemes.value?.count() ?: 0)) {
                        withStyle(
                            style = style.copy(
                                color = style.color.copy(alpha = if(isTicked.value) 1f else 0f),
                                letterSpacing = 0.sp
                            ).toSpanStyle()
                        ) {
                            append("|")
                        }
                    }
                    withStyle(
                        style.toSpanStyle().copy(
                            color = when {
                                index < currentPosition -> style.color
                                else -> style.color.copy(alpha = .4f)
                            },
                            fontWeight = if(index < currentPosition
                                && aboveMedianIndexes.value?.contains(index) == true
                            ) {
                                FontWeight.ExtraBold
                            }else null
                        )
                    ) {
                        append(matchResult.value)
                    }
                }
            }
        }
    }
}

private const val FLICKER_DELAY_LOOSE = 400L
private const val FULL_STOP_LENGTH = 800L
