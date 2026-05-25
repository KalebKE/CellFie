package org.caexplorer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cellfie.shared.generated.resources.Res
import cellfie.shared.generated.resources.oxanium_variable
import cellfie.shared.generated.resources.vt323_regular
import org.jetbrains.compose.resources.Font

/** Oxanium — sci-fi display font for headings and titles. */
val DisplayFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.oxanium_variable, weight = FontWeight.Normal),
        Font(Res.font.oxanium_variable, weight = FontWeight.Medium),
        Font(Res.font.oxanium_variable, weight = FontWeight.SemiBold),
        Font(Res.font.oxanium_variable, weight = FontWeight.Bold),
    )

/** VT323 — pixel/monospace font for body text and labels. */
val BodyFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.vt323_regular, weight = FontWeight.Normal),
    )

private val baseline = Typography()

val CATypography: Typography
    @Composable get() {
        val display = DisplayFontFamily
        val body = BodyFontFamily
        return Typography(
            displayLarge = baseline.displayLarge.copy(fontFamily = display),
            displayMedium = baseline.displayMedium.copy(fontFamily = display),
            displaySmall = baseline.displaySmall.copy(fontFamily = display),
            headlineLarge = baseline.headlineLarge.copy(fontFamily = display),
            headlineMedium = baseline.headlineMedium.copy(fontFamily = display),
            headlineSmall = baseline.headlineSmall.copy(fontFamily = display),
            titleLarge = baseline.titleLarge.copy(fontFamily = display),
            titleMedium = baseline.titleMedium.copy(fontFamily = display),
            titleSmall = baseline.titleSmall.copy(fontFamily = display),
            bodyLarge = baseline.bodyLarge.copy(fontFamily = body),
            bodyMedium = baseline.bodyMedium.copy(fontFamily = body),
            bodySmall = baseline.bodySmall.copy(fontFamily = body),
            labelLarge = baseline.labelLarge.copy(fontFamily = body),
            labelMedium = baseline.labelMedium.copy(fontFamily = body),
            labelSmall = baseline.labelSmall.copy(fontFamily = body),
        )
    }
