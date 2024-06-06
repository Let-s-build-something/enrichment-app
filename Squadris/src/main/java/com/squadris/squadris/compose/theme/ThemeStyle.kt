package com.squadris.squadris.compose.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/** Variables with default styles */
interface ThemeStyle {

    /** default colors for material3 text input */
    @get:Composable
    val textFieldColors: TextFieldColors

    /** only focusable colors for material3 text input */
    @get:Composable
    val textFieldColorsOnFocus: TextFieldColors

    /** default colors for material3 checkbox */
    @get:Composable
    val checkBoxColorsDefault: CheckboxColors

    /** default colors for material3 checkbox */
    @get:Composable
    val switchColorsDefault: SwitchColors

    val componentElevation: Dp
    val actionElevation: Dp
    val minimumElevation: Dp

    @get:Composable
    val cardClickableElevation: CardElevation

    @get:Composable
    val chipBorderDefault: BorderStroke

    @get:Composable
    val chipColorsDefault: SelectableChipColors

    /** Large, bold, heading style for text */
    @get:Composable
    val heading: TextStyle

    /** Medium, bold, subheading style for text */
    @get:Composable
    val subheading: TextStyle

    /** Medium, thin, category style for text */
    @get:Composable
    val category: TextStyle

    /** Style for link text */
    @get:Composable
    val linkText: TextStyle

    /** Style for item in a menu */
    @get:Composable
    val menuItem: TextStyle
}