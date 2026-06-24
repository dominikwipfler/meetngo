package com.meetngo.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Animierter "Shimmer"-Hintergrund für Lade-Platzhalter (Skeletons). Ein heller Streifen wandert
 * fortlaufend über die Fläche und vermittelt so – moderner als ein Spinner – dass Inhalte laden.
 * Als Modifier konzipiert, damit beliebige Platzhalter-Boxen damit gefüllt werden können.
 */
@Composable
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.9f), base.copy(alpha = 0.3f), base.copy(alpha = 0.9f)),
        start = Offset(translate - 600f, 0f),
        end = Offset(translate, 0f),
    )
    return this.clip(shape).background(brush)
}

/** Platzhalter-Karte in der Form der Such-Trefferkarte (Bild-Thumbnail + drei Textzeilen). */
@Composable
fun EventCardSkeleton(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shimmer(RoundedCornerShape(12.dp)),
            )
            Column(
                modifier = Modifier.padding(start = 12.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                SkeletonLine(widthFraction = 0.7f, height = 16.dp)
                SkeletonLine(widthFraction = 0.5f, height = 12.dp)
                SkeletonLine(widthFraction = 0.4f, height = 12.dp)
            }
        }
    }
}

/** Einzelne Platzhalter-Textzeile mit Shimmer. */
@Composable
fun SkeletonLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .shimmer(RoundedCornerShape(50)),
    )
}
