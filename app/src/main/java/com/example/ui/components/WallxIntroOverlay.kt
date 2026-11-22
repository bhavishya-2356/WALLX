package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ChromeSilver
import com.example.ui.theme.LocalWallxDarkTheme
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay

/**
 * Visible, silky-smooth ~2.1s Liquid Glass GPU startup animation.
 *
 * Waits briefly for the Android launch splash transition to clear so the animation is ALWAYS
 * seen clearly on every device, then drives cached Liquid Glass prism rings, luminous orb,
 * and kinetic WALLX reveal from a single Animatable(0f..1f) purely on the GPU.
 */
@Composable
fun WallxIntroOverlay(
    onMountUnderlay: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val isDark = LocalWallxDarkTheme.current

    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onPrimaryColor = colorScheme.onPrimary
    val outlineColor = colorScheme.outline
    val secondaryTextColor = colorScheme.onSurfaceVariant

    LaunchedEffect(Unit) {
        // Wait for Android's launcher splash transition so users ALWAYS see the full animation
        withFrameNanos { }
        withFrameNanos { }
        delay(220)

        // Phase 1 (0f -> 0.78f over 1520ms): Pure isolated GPU-only Liquid Glass intro
        progress.animateTo(
            targetValue = 0.78f,
            animationSpec = tween(durationMillis = 1520, easing = LinearEasing)
        )

        // Mount main app cleanly right as the smooth fade/lift reveal starts
        onMountUnderlay()
        withFrameNanos { }

        // Phase 2 (0.78f -> 1.00f over 360ms): Silky GPU fade-out reveal
        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 360, easing = LinearEasing)
        )
        onFinished()
    }

    val titleText = remember {
        buildAnnotatedString {
            append("WALL")
            withStyle(SpanStyle(color = ChromeSilver)) {
                append("X")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val exit = easeOutCubic(segment(progress.value, 0.78f, 1.00f))
                alpha = 1f - exit
                val s = 1f + 0.04f * exit
                scaleX = s
                scaleY = s
            }
            .wallxBackdrop(),
        contentAlignment = Alignment.Center
    ) {
        // Frosted Liquid Glass central disc (cached once, animated purely on GPU)
        Box(
            modifier = Modifier
                .size(236.dp)
                .graphicsLayer {
                    val t = progress.value
                    val e = easeOutCubic(segment(t, 0.00f, 0.34f))
                    alpha = e
                    val s = 0.76f + 0.24f * e
                    scaleX = s
                    scaleY = s
                }
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val discRadius = size.minDimension * 0.45f
                    val glassFill = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0xB8252B38), Color(0x8810131A))
                        } else {
                            listOf(Color(0xE0FFFFFF), Color(0xB8E6ECF5))
                        },
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    val rimGradient = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0x88FFFFFF), Color(0x229BA4B5), Color(0x55FFFFFF))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0x88B4BFCF), Color(0xE6FFFFFF))
                        },
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    val rimStroke = Stroke(width = 1.6.dp.toPx())
                    onDrawBehind {
                        drawCircle(brush = glassFill, radius = discRadius, center = center)
                        drawCircle(
                            brush = rimGradient,
                            radius = discRadius,
                            center = center,
                            style = rimStroke
                        )
                    }
                }
        )

        // Outer orbiting Liquid Glass silver-chrome prism arc (pure GPU rotation)
        Box(
            modifier = Modifier
                .size(268.dp)
                .graphicsLayer {
                    val t = progress.value
                    val e = easeOutCubic(segment(t, 0.00f, 0.30f))
                    alpha = e
                    val s = 0.82f + 0.18f * e
                    scaleX = s
                    scaleY = s
                    rotationZ = -35f + 240f * t
                }
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.46f
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    val primaryArc = primaryColor.copy(alpha = 0.90f)
                    val silverArc = ChromeSilver.copy(alpha = 0.72f)
                    onDrawBehind {
                        drawArc(
                            color = primaryArc,
                            startAngle = 0f,
                            sweepAngle = 108f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                        drawArc(
                            color = silverArc,
                            startAngle = 180f,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }
        )

        // Inner counter-rotating silver specular arc (pure GPU rotation)
        Box(
            modifier = Modifier
                .size(268.dp)
                .graphicsLayer {
                    val t = progress.value
                    val e = easeOutCubic(segment(t, 0.04f, 0.34f))
                    alpha = e
                    val s = 0.80f + 0.20f * e
                    scaleX = s
                    scaleY = s
                    rotationZ = 50f - 210f * t
                }
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.40f
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    val innerArc = secondaryTextColor.copy(alpha = 0.58f)
                    onDrawBehind {
                        drawArc(
                            color = innerArc,
                            startAngle = 30f,
                            sweepAngle = 115f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }
        )

        // Kinetic WALLX Typography & Liquid Glass 8K Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titleText,
                style = TextStyle(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp,
                    letterSpacing = 5.sp,
                    color = primaryColor
                ),
                modifier = Modifier.graphicsLayer {
                    val e = easeOutCubic(segment(progress.value, 0.06f, 0.36f))
                    alpha = e
                    translationY = 18f * (1f - e)
                    val s = 0.90f + 0.10f * e
                    scaleX = s
                    scaleY = s
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer {
                    val b = easeOutCubic(segment(progress.value, 0.18f, 0.46f))
                    alpha = b
                    translationY = 14f * (1f - b)
                }
            ) {
                LiquidGlassSurface(
                    style = LiquidGlassStyle.MINT_PRIMARY,
                    shape = CircleShape,
                    cornerRadius = 18.dp
                ) {
                    Text(
                        text = stringResource(R.string.intro_quality_tag),
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.2.sp),
                        color = onPrimaryColor,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.intro_wallpaper_name),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.6.sp),
                    color = secondaryTextColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Liquid Glass Progress Capsule (pure GPU scaleX)
        LiquidGlassSurface(
            style = LiquidGlassStyle.FROST_WHITE,
            shape = CircleShape,
            cornerRadius = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .width(136.dp)
                .height(6.dp)
                .graphicsLayer {
                    alpha = easeOutCubic(segment(progress.value, 0.10f, 0.34f))
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(outlineColor.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            scaleX = easeOutCubic(segment(progress.value, 0.06f, 0.78f)).coerceIn(0.02f, 1f)
                        }
                        .background(primaryColor)
                )
            }
        }
    }
}

private fun segment(t: Float, start: Float, end: Float): Float {
    if (end <= start) return 1f
    return ((t - start) / (end - start)).coerceIn(0f, 1f)
}

private fun easeOutCubic(x: Float): Float {
    val inv = 1f - x
    return 1f - inv * inv * inv
}
