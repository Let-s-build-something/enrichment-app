package com.squadris.squadris.ext

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun isTablet(activity: Activity): Boolean {
    return calculateWindowSizeClass(activity = activity).widthSizeClass != WindowWidthSizeClass.Compact
}