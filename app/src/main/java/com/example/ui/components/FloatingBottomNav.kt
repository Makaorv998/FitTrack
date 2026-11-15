package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.FitTrackRoute
import com.example.ui.theme.FitTrackRadius
import com.example.ui.theme.LocalFitTrackColors

data class BottomNavTabItem(
    val route: FitTrackRoute,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

@Composable
fun FloatingBottomNavigationBar(
    tabs: List<BottomNavTabItem>,
    isSelected: (FitTrackRoute) -> Boolean,
    onSelect: (FitTrackRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFitTrackColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (ext.isDark) 16.dp else 10.dp,
                    shape = RoundedCornerShape(FitTrackRadius.xl),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(FitTrackRadius.xl),
            color = if (ext.isDark) ext.surfaceElevated.copy(alpha = 0.96f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ext.border.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = isSelected(tab.route)
                    val reducedMotion = com.example.ui.theme.rememberReducedMotion()
                    val tint by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "tab_tint"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected && !reducedMotion) 1.04f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(FitTrackRadius.lg))
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(tab.route) }
                            )
                            .semantics { contentDescription = tab.label }
                            .testTag("nav_${tab.label.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingAIButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .shadow(12.dp, CircleShape),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Open AI Fitness Coach",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
