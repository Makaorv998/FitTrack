package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FitTrackSpacing
import com.example.ui.theme.LocalFitTrackColors
import com.example.ui.theme.rememberReducedMotion

@Composable
fun rememberAuroraShimmerBrush(): Brush {
    val ext = LocalFitTrackColors.current
    val base = ext.surfaceElevated
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = if (ext.isDark) 0.16f else 0.10f)
    val reducedMotion = rememberReducedMotion()

    if (reducedMotion) {
        return Brush.linearGradient(listOf(base.copy(alpha = 0.65f), base.copy(alpha = 0.85f)))
    }

    val transition = rememberInfiniteTransition(label = "aurora_shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    return Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = 0.55f),
            highlight,
            base.copy(alpha = 0.55f)
        ),
        start = Offset(translate - 320f, 0f),
        end = Offset(translate, 260f)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    cornerRadius: Dp = 10.dp
) {
    val brush = rememberAuroraShimmerBrush()
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    contentHeight: Dp = 56.dp
) {
    FitTrackCard(modifier = modifier.fillMaxWidth()) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.45f), height = 16.dp)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = contentHeight, cornerRadius = 12.dp)
    }
}

@Composable
fun SkeletonLoaderCard(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier)
}

@Composable
fun SkeletonDashboard(
    statusLabel: String = "Syncing your FitTrack dashboard...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FitTrackSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FitTrackSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(modifier = Modifier.width(140.dp), height = 14.dp)
                ShimmerBox(modifier = Modifier.width(200.dp), height = 24.dp)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(rememberAuroraShimmerBrush())
            )
        }

        // Hero ring skeleton matching dashboard ring card dimensions
        FitTrackCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .background(rememberAuroraShimmerBrush())
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerBox(modifier = Modifier.width(105.dp), height = 20.dp)
                    ShimmerBox(modifier = Modifier.width(88.dp), height = 14.dp)
                    ShimmerBox(modifier = Modifier.width(116.dp), height = 14.dp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCard(modifier = Modifier.weight(1f), contentHeight = 44.dp)
            SkeletonCard(modifier = Modifier.weight(1f), contentHeight = 44.dp)
        }

        SkeletonProgressCharts()

        Text(
            text = statusLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun SkeletonActivityStats(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(contentHeight = 110.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCard(modifier = Modifier.weight(1f), contentHeight = 48.dp)
            SkeletonCard(modifier = Modifier.weight(1f), contentHeight = 48.dp)
        }
    }
}

@Composable
fun SkeletonNutritionDashboard(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(contentHeight = 96.dp)
        SkeletonFoodList()
    }
}

@Composable
fun SkeletonFoodList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            SkeletonCard(contentHeight = 42.dp)
        }
    }
}

@Composable
fun SkeletonWorkoutList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            SkeletonCard(contentHeight = 64.dp)
        }
    }
}

@Composable
fun SkeletonExerciseDetails(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier, contentHeight = 140.dp)
}

@Composable
fun SkeletonProgressCharts(modifier: Modifier = Modifier) {
    SkeletonCard(modifier = modifier, contentHeight = 128.dp)
}

@Composable
fun SkeletonProfile(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(contentHeight = 76.dp)
        SkeletonCard(contentHeight = 120.dp)
    }
}

@Composable
fun SkeletonWorkoutDashboard(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(contentHeight = 132.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCard(modifier = Modifier.weight(1f), contentHeight = 68.dp)
            SkeletonCard(modifier = Modifier.weight(1f), contentHeight = 68.dp)
        }
        SkeletonWorkoutList()
    }
}

@Composable
fun SkeletonActiveWorkout(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(contentHeight = 92.dp)
        SkeletonCard(contentHeight = 180.dp)
        SkeletonCard(contentHeight = 110.dp)
    }
}

@Composable
fun SkeletonCalendar(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(contentHeight = 180.dp)
        SkeletonCard(contentHeight = 84.dp)
    }
}

@Composable
fun SkeletonActivity(modifier: Modifier = Modifier) = SkeletonActivityStats(modifier = modifier)

@Composable
fun SkeletonNutrition(modifier: Modifier = Modifier) = SkeletonNutritionDashboard(modifier = modifier)


