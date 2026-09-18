package com.ammu.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ammu.player.ui.theme.DarkBorder
import kotlinx.coroutines.delay

@Composable
fun VinylDiscView(
    artworkUri: String?,
    isPlaying: Boolean,
    isVinylMode: Boolean,
    onToggleVinylMode: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(500)
            seekFeedbackText = null
        }
    }

    // Continuous rotation for vinyl mode
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val currentRotation = if (isVinylMode && isPlaying) rotationAngle else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(240.dp)
            .shadow(16.dp, if (isVinylMode) CircleShape else RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val isRight = offset.x > size.width / 2
                        if (isRight) {
                            onSeekRelative(10_000L)
                            seekFeedbackText = "⏩ +10s"
                        } else {
                            onSeekRelative(-10_000L)
                            seekFeedbackText = "⏪ -10s"
                        }
                    },
                    onTap = {
                        onToggleVinylMode()
                    }
                )
            }
    ) {
        if (isVinylMode) {
            // Vinyl Outer Disc with Grooves
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(currentRotation)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0F0F))
                    .border(4.dp, Color(0xFF1F1F1F), CircleShape)
            ) {
                // Vinyl Grooves Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = size.width / 2
                    val grooveCount = 18

                    for (i in 4..grooveCount) {
                        val radius = (maxRadius * (i.toFloat() / grooveCount)) - 6f
                        drawCircle(
                            color = Color(0x33262626),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // Vinyl Sheen / Reflection
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0x1AFFFFFF),
                                Color(0x00FFFFFF),
                                Color(0x2AFFFFFF),
                                Color(0x00FFFFFF),
                                Color(0x1AFFFFFF)
                            ),
                            center = center
                        ),
                        radius = maxRadius
                    )
                }

                // Center Album Label
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF333333), CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Label",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Spindle Center Hole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0D14))
                            .border(2.dp, Color(0xFF222222), CircleShape)
                    )
                }
            }
        } else {
            // Standard Rounded Album Artwork
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Floating Seek Feedback Overlay
        if (seekFeedbackText != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000), if (isVinylMode) CircleShape else RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = seekFeedbackText!!,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
