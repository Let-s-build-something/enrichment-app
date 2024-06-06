package com.squadris.squadris.ext

import java.math.RoundingMode
import java.text.DecimalFormat

/** rounds a double into two decimals */
fun Float.roundOffDecimal(): Float {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.CEILING
    return df.format(this).toFloatOrNull() ?: this
}