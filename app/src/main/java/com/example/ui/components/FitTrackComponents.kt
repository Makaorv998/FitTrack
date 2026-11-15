package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.branding.FitTrackGeometricIllustration
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.theme.FitTrackMotion
import com.example.ui.theme.FitTrackRadius
import com.example.ui.theme.FitTrackSizes
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.rememberReducedMotion

@Composable
fun AuroraAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val ext = LocalFitTrackColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val alphaTop = if (ext.isDark) 0.14f else 0.08f
            val alphaSide = if (ext.isDark) 0.10f else 0.06f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = alphaTop), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.06f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.15f, size.height * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = alphaSide), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.24f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.90f, size.height * 0.24f)
            )
        }
        content()
    }
}

@Composable
fun FitTrackCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentBorder: Color? = null,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalFitTrackColors.current
    val borderColor = accentBorder ?: ext.border.copy(alpha = if (ext.isDark) 0.75f else 0.9f)
    val container = if (elevated) ext.surfaceElevated else MaterialTheme.colorScheme.surface
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (ext.isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentBorder: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalFitTrackColors.current
    val borderColor = accentBorder ?: ext.border.copy(alpha = 0.75f)
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick) else Modifier
        ),
        shape = MaterialTheme.shapes.large,
        color = ext.glassSurface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun ActivityCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentBorder: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) = FitTrackCard(modifier = modifier, onClick = onClick, accentBorder = accentBorder, elevated = true, content = content)

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) = MetricStatTile(title, value, subtitle, icon, tint, modifier, onClick)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(FitTrackMotion.durationFast),
        label = "btn_scale"
    )
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .height(FitTrackSizes.buttonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, LocalFitTrackColors.current.border),
        modifier = modifier.height(FitTrackSizes.buttonHeight)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(FitTrackMotion.durationFast),
        label = "grad_btn_scale"
    )
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
    } else {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .height(FitTrackSizes.buttonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimatedCounterText(
    targetValue: Int,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    suffix: String = ""
) {
    val reducedMotion = rememberReducedMotion()
    val animated by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else FitTrackMotion.durationSlow,
            easing = FastOutSlowInEasing
        ),
        label = "count_up"
    )
    Text(text = "$animated$suffix", style = style, color = color)
}

@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    strokeWidth: Dp = 11.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    centerContent: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else FitTrackMotion.durationSlow,
            easing = FastOutSlowInEasing
        ),
        label = "ring_progress"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        centerContent()
    }
}

@Composable
fun AuroraProgressBar(
    label: String,
    valueText: String,
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (reducedMotion) 0 else 450),
        label = "bar_progress"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MetricStatTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    FitTrackCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FitTrackAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = FitTrackSizes.avatarSm,
    modifier: Modifier = Modifier
) {
    val brush = LocalFitTrackColors.current.auroraBrush
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "$displayName avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = (displayName.firstOrNull() ?: 'A').uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(FitTrackRadius.pill),
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FitTrackRadius.md),
        color = ext.surfaceElevated,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                Surface(
                    onClick = { onSelect(idx) },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(FitTrackRadius.sm),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun DisclaimerBanner(
    text: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    val tint = if (isWarning) ext.warning else MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Information",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SuccessStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ext.success.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, ext.success.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "Success", tint = ext.success)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_error_button")
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    illustration: FitTrackIllustrationType? = null,
    modifier: Modifier = Modifier
) {
    FitTrackCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (illustration != null) {
                FitTrackGeometricIllustration(type = illustration, size = 104.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth(0.7f))
            }
        }
    }
}

@Composable
fun ConfirmationModal(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTrackBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        content = content
    )
}

@Composable
fun SimpleBarChart(
    values: List<Pair<String, Float>>,
    barColor: Color = MaterialTheme.colorScheme.primary,
    secondaryBarColor: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return
    val maxVal = (values.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    val reducedMotion = rememberReducedMotion()
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 550, easing = FastOutSlowInEasing),
        label = "bar_reveal"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
        ) {
            val count = values.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.46f).coerceAtLeast(10f)
            val cornerRadius = 10f

            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = Offset(0f, size.height - 2f),
                end = Offset(size.width, size.height - 2f),
                strokeWidth = 2f
            )

            values.forEachIndexed { index, (_, v) ->
                val ratio = ((v / maxVal).coerceIn(0f, 1f)) * reveal
                val barHeight = (size.height - 14f) * ratio
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight - 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(secondaryBarColor, barColor)),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            values.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TrendLineChart(
    points: List<Pair<String, Float>>,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val minVal = points.minOf { it.second }
    val maxVal = points.maxOf { it.second }
    val span = (maxVal - minVal).coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
        ) {
            val padH = 16f
            val padV = 16f
            val usableW = (size.width - padH * 2).coerceAtLeast(10f)
            val usableH = (size.height - padV * 2).coerceAtLeast(10f)

            val coords = points.mapIndexed { idx, (_, value) ->
                val x = if (points.size == 1) size.width / 2f else padH + (idx.toFloat() / (points.size - 1)) * usableW
                val normY = (value - minVal) / span
                val y = padV + (1f - normY) * usableH
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val path = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(coords.last().x, size.height)
                    lineTo(coords.first().x, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = lineColor, radius = 7f, center = pt)
                drawCircle(color = Color.White, radius = 3.5f, center = pt)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${points.first().first} (${String.format("%.1f", points.first().second)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (points.size > 1) {
                Text(
                    text = "${points.last().first} (${String.format("%.1f", points.last().second)})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = lineColor
                )
            }
        }
    }
}
