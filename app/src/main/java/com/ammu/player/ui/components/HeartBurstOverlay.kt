package com.ammu.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class HeartParticle(
    val id: Int,
    val initialX: Float,
    val initialY: Float,
    val targetY: Float,
    val sizeSp: Float
)

@Composable
fun HeartBurstOverlay(
    isFavorited: Boolean,
    triggerKey: Long,
    modifier: Modifier = Modifier
) {
    if (triggerKey == 0L) return

    var visible by remember(triggerKey) { mutableStateOf(true) }

    LaunchedEffect(triggerKey) {
        visible = true
        delay(1500)
        visible = false
    }

    if (!visible) return

    val particles = remember(triggerKey) {
        List(18) { index ->
            HeartParticle(
                id = index,
                initialX = Random.nextFloat() * 280f - 140f,
                initialY = Random.nextFloat() * 100f,
                targetY = -(Random.nextFloat() * 250f + 100f),
                sizeSp = Random.nextFloat() * 14f + 22f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "HeartBurst")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Progress"
    )

    val heartEmoji = if (isFavorited) "❤️" else "💛"

    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val curY = p.initialY + (p.targetY - p.initialY) * progress
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val scale = (0.6f + progress * 0.8f).coerceIn(0.6f, 1.4f)

            Text(
                text = heartEmoji,
                fontSize = p.sizeSp.sp,
                modifier = Modifier
                    .offset { IntOffset(p.initialX.dp.roundToPx(), curY.dp.roundToPx()) }
                    .alpha(alpha)
                    .scale(scale)
            )
        }
    }
}
