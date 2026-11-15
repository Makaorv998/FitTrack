package com.example.ui.branding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkSecondary
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.rememberReducedMotion

enum class FitTrackLogoVariant {
    FULL_COLOR,
    MONOCHROME,
    DARK_BG,
    LIGHT_BG
}

enum class FitTrackIllustrationType {
    WALKING,
    RUNNING,
    STRENGTH,
    NUTRITION,
    HYDRATION,
    PROGRESS,
    AI_COACH,
    ACHIEVEMENTS
}

@Composable
fun FitTrackLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    variant: FitTrackLogoVariant = FitTrackLogoVariant.FULL_COLOR,
    animateRing: Boolean = false
) {
    val reducedMotion = rememberReducedMotion()
    val infinite = rememberInfiniteTransition(label = "logo_orbit")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val ringColors = when (variant) {
        FitTrackLogoVariant.MONOCHROME -> listOf(
            MaterialTheme.colorScheme.onBackground,
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        else -> listOf(DarkPrimary, DarkSecondary)
    }
    val fColorTop = when (variant) {
        FitTrackLogoVariant.MONOCHROME -> MaterialTheme.colorScheme.onBackground
        FitTrackLogoVariant.LIGHT_BG -> Color(0xFF101828)
        else -> Color(0xFFF8FAFC)
    }
    val fColorBottom = when (variant) {
        FitTrackLogoVariant.MONOCHROME -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        FitTrackLogoVariant.LIGHT_BG -> Color(0xFF0891B2)
        else -> Color(0xFFA5F3FC)
    }
    val pulseAccent = when (variant) {
        FitTrackLogoVariant.MONOCHROME -> MaterialTheme.colorScheme.onBackground
        FitTrackLogoVariant.LIGHT_BG -> Color(0xFF7C3AED)
        else -> DarkSecondary
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        val inset = stroke * 0.85f
        val arcSize = Size(w - inset * 2f, h - inset * 2f)

        // Fitness orbital arc (broken at top-left for forward kinetic momentum)
        val rotation = if (animateRing && !reducedMotion) orbitAngle else 0f
        rotate(degrees = rotation, pivot = Offset(w / 2f, h / 2f)) {
            drawArc(
                brush = Brush.sweepGradient(ringColors + ringColors.first()),
                startAngle = -55f,
                sweepAngle = 295f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Stylized forward-leaning letter F with aerodynamic speed bevel
        val fPath = Path().apply {
            moveTo(w * 0.34f, h * 0.72f)
            lineTo(w * 0.415f, h * 0.30f)
            lineTo(w * 0.70f, h * 0.30f)
            lineTo(w * 0.665f, h * 0.405f)
            lineTo(w * 0.495f, h * 0.405f)
            lineTo(w * 0.478f, h * 0.485f)
            lineTo(w * 0.635f, h * 0.485f)
            lineTo(w * 0.605f, h * 0.58f)
            lineTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.435f, h * 0.72f)
            close()
        }
        drawPath(
            path = fPath,
            brush = Brush.linearGradient(
                colors = listOf(fColorTop, fColorBottom),
                start = Offset(w * 0.34f, h * 0.30f),
                end = Offset(w * 0.70f, h * 0.72f)
            )
        )

        // Subtle pulse chevron / motion spark on lower right
        val pulsePath = Path().apply {
            moveTo(w * 0.66f, h * 0.63f)
            lineTo(w * 0.72f, h * 0.57f)
            lineTo(w * 0.78f, h * 0.63f)
            lineTo(w * 0.72f, h * 0.69f)
            close()
        }
        drawPath(path = pulsePath, color = pulseAccent)
    }
}

@Composable
fun FitTrackHorizontalLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 40.dp,
    showSlogan: Boolean = false,
    variant: FitTrackLogoVariant? = null
) {
    val ext = LocalFitTrackColors.current
    val resolvedVariant = variant ?: if (ext.isDark) FitTrackLogoVariant.DARK_BG else FitTrackLogoVariant.LIGHT_BG

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FitTrackLogoMark(size = markSize, variant = resolvedVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "FITTRACK",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showSlogan) {
                Text(
                    text = "Move Smarter. Become Stronger.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun FitTrackSplashLogo(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    var revealed by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) {
        revealed = true
    }
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.78f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 520, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 480),
        label = "splash_alpha"
    )

    Column(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(164.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DarkPrimary.copy(alpha = 0.34f),
                            DarkSecondary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
            }
            FitTrackLogoMark(
                size = 104.dp,
                variant = FitTrackLogoVariant.DARK_BG,
                animateRing = true
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "FITTRACK",
            style = MaterialTheme.typography.displayLarge.copy(
                letterSpacing = 3.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color(0xFFF8FAFC)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Move Smarter. Become Stronger.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF06B6D4)
        )
    }
}

@Composable
fun FitTrackGeometricIllustration(
    type: FitTrackIllustrationType,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp
) {
    val ext = LocalFitTrackColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val success = ext.success
    val warning = ext.warning
    val surface = ext.surfaceElevated

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)

        // Subtle ambient aurora disc behind every illustration for unified brand style
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.24f),
                    secondary.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = center
        )
        drawCircle(
            color = surface.copy(alpha = 0.7f),
            radius = w * 0.34f,
            center = center
        )
        drawCircle(
            brush = Brush.linearGradient(listOf(primary.copy(alpha = 0.55f), secondary.copy(alpha = 0.55f))),
            radius = w * 0.34f,
            center = center,
            style = Stroke(width = 2.5f)
        )

        when (type) {
            FitTrackIllustrationType.WALKING -> {
                // Kinetic footsteps / stride arcs + forward path
                drawArc(
                    brush = Brush.linearGradient(listOf(primary, secondary)),
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.24f),
                    size = Size(w * 0.56f, h * 0.52f),
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )
                drawRoundRect(
                    color = secondary,
                    topLeft = Offset(w * 0.33f, h * 0.36f),
                    size = Size(w * 0.12f, h * 0.22f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.54f, h * 0.44f),
                    size = Size(w * 0.12f, h * 0.22f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }

            FitTrackIllustrationType.RUNNING -> {
                // Forward velocity chevrons + GPS route node
                val chevron1 = Path().apply {
                    moveTo(w * 0.30f, h * 0.34f)
                    lineTo(w * 0.48f, h * 0.50f)
                    lineTo(w * 0.30f, h * 0.66f)
                }
                val chevron2 = Path().apply {
                    moveTo(w * 0.48f, h * 0.34f)
                    lineTo(w * 0.66f, h * 0.50f)
                    lineTo(w * 0.48f, h * 0.66f)
                }
                drawPath(chevron1, color = primary, style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(chevron2, color = secondary, style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(color = success, radius = 8f, center = Offset(w * 0.72f, h * 0.36f))
            }

            FitTrackIllustrationType.STRENGTH -> {
                // Geometric kinetic bar & balanced plates
                drawLine(
                    brush = Brush.horizontalGradient(listOf(primary, secondary)),
                    start = Offset(w * 0.24f, h * 0.50f),
                    end = Offset(w * 0.76f, h * 0.50f),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w * 0.27f, h * 0.34f),
                    size = Size(w * 0.09f, h * 0.32f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = secondary,
                    topLeft = Offset(w * 0.64f, h * 0.34f),
                    size = Size(w * 0.09f, h * 0.32f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            FitTrackIllustrationType.NUTRITION -> {
                // Macro triad rings (Protein / Carbs / Fat)
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = secondary,
                    startAngle = 35f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = warning,
                    startAngle = 160f,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )
            }

            FitTrackIllustrationType.HYDRATION -> {
                // Geometric water drop + wave level
                val drop = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    cubicTo(w * 0.67f, h * 0.48f, w * 0.66f, h * 0.72f, w * 0.50f, h * 0.72f)
                    cubicTo(w * 0.34f, h * 0.72f, w * 0.33f, h * 0.48f, w * 0.50f, h * 0.26f)
                    close()
                }
                drawPath(
                    path = drop,
                    brush = Brush.verticalGradient(listOf(secondary, primary))
                )
            }

            FitTrackIllustrationType.PROGRESS -> {
                // Rising telemetry bars + trend ray
                val heights = listOf(0.18f, 0.28f, 0.36f, 0.46f)
                heights.forEachIndexed { idx, frac ->
                    val bx = w * (0.28f + idx * 0.12f)
                    val bh = h * frac
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondary, primary)),
                        topLeft = Offset(bx, h * 0.70f - bh),
                        size = Size(w * 0.08f, bh),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            FitTrackIllustrationType.AI_COACH -> {
                // Aurora neural pulse star + orbital halo
                val star = Path().apply {
                    moveTo(w * 0.50f, h * 0.24f)
                    quadraticTo(w * 0.54f, h * 0.46f, w * 0.76f, h * 0.50f)
                    quadraticTo(w * 0.54f, h * 0.54f, w * 0.50f, h * 0.76f)
                    quadraticTo(w * 0.46f, h * 0.54f, w * 0.24f, h * 0.50f)
                    quadraticTo(w * 0.46f, h * 0.46f, w * 0.50f, h * 0.24f)
                    close()
                }
                drawPath(
                    path = star,
                    brush = Brush.linearGradient(listOf(primary, secondary))
                )
            }

            FitTrackIllustrationType.ACHIEVEMENTS -> {
                // Hexagon victory prism
                val hex = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    lineTo(w * 0.69f, h * 0.38f)
                    lineTo(w * 0.69f, h * 0.62f)
                    lineTo(w * 0.50f, h * 0.74f)
                    lineTo(w * 0.31f, h * 0.62f)
                    lineTo(w * 0.31f, h * 0.38f)
                    close()
                }
                drawPath(
                    path = hex,
                    brush = Brush.linearGradient(listOf(primary.copy(alpha = 0.35f), secondary.copy(alpha = 0.35f)))
                )
                drawPath(
                    path = hex,
                    brush = Brush.linearGradient(listOf(primary, secondary)),
                    style = Stroke(width = 6f, join = StrokeJoin.Round)
                )
                drawCircle(color = success, radius = w * 0.08f, center = center)
            }
        }
    }
}
