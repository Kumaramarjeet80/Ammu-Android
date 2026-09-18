package com.ammu.player.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.ui.theme.AccentGreen
import com.ammu.player.ui.theme.AccentGreenLight
import com.ammu.player.ui.theme.DarkCard
import kotlinx.coroutines.delay

@Composable
fun EmotionalToast(
    isLiked: Boolean,
    triggerKey: Long,
    modifier: Modifier = Modifier
) {
    if (triggerKey == 0L) return

    var visible by remember(triggerKey) { mutableStateOf(true) }

    LaunchedEffect(triggerKey) {
        visible = true
        delay(2800)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + scaleIn(tween(350, easing = OvershootInterpolator(1.5f).toEasing())),
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            val emoji = if (isLiked) "🥺" else "😿"
            val title = if (isLiked) "Thank you for loving me 🥺" else "Dil tod diya na mera 😿"

            // Bouncing big emoji
            val bounceTransition = rememberInfiniteTransition(label = "EmojiBounce")
            val bounceScale by bounceTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Bounce"
            )

            Text(
                text = emoji,
                fontSize = 64.sp,
                modifier = Modifier
                    .scale(bounceScale)
                    .offset(y = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGreen),
                modifier = Modifier
                    .shadow(24.dp, RoundedCornerShape(20.dp))
                    .padding(top = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "From Amarjeet",
                        color = AccentGreenLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private class OvershootInterpolator(private val tension: Float = 2.0f) {
    fun toEasing(): Easing = Easing { t ->
        val t1 = t - 1.0f
        t1 * t1 * ((tension + 1) * t1 + tension) + 1.0f
    }
}
