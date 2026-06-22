package com.meetngo.app.ui.screens.auth

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.meetngo.app.ui.theme.MeetNGoColors

/**
 * Shared text-field styling for the login and register forms: a filled
 * [MeetNGoColors.BrandLight] background with no visible indicator line.
 *
 * Gemeinsames Textfeld-Styling für die Login- und Registrierungs-Formulare:
 * gefüllter [MeetNGoColors.BrandLight]-Hintergrund ohne sichtbare Unterstrich-Linie,
 * passend zum Design der Web-App.
 */
val filledFieldColors
    @Composable get() = TextFieldDefaults.colors(
        focusedContainerColor = MeetNGoColors.BrandLight,
        unfocusedContainerColor = MeetNGoColors.BrandLight,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
