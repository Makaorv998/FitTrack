package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LocalFitTrackColors

@Stable
class LocationPermissionState(
    val hasFineLocation: Boolean,
    val hasCoarseLocation: Boolean,
    val shouldShowRationale: Boolean,
    val permanentlyDenied: Boolean,
    val requestPermissions: () -> Unit,
    val openAppSettings: () -> Unit
) {
    val hasAnyLocation: Boolean
        get() = hasFineLocation || hasCoarseLocation
}

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Remembers and manages Android Fine (`ACCESS_FINE_LOCATION`) and Coarse (`ACCESS_COARSE_LOCATION`)
 * runtime permissions, re-evaluating automatically when the host lifecycle resumes.
 */
@Composable
fun rememberLocationPermissionState(
    includeNotificationsOnTiramisu: Boolean = true,
    onResult: (hasFine: Boolean, hasCoarse: Boolean) -> Unit = { _, _ -> }
): LocationPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasFine by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var hasCoarse by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    var hasRequestedOnce by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasFine = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        hasCoarse = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsToRequest = remember(includeNotificationsOnTiramisu) {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (includeNotificationsOnTiramisu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasRequestedOnce = true
        val fineGranted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseGranted = (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) ||
            context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        hasFine = fineGranted
        hasCoarse = coarseGranted
        onResult(fineGranted, coarseGranted)
    }

    val shouldShowRationale = if (activity != null) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        false
    }

    val permanentlyDenied = hasRequestedOnce && !hasFine && !hasCoarse && !shouldShowRationale

    return LocationPermissionState(
        hasFineLocation = hasFine,
        hasCoarseLocation = hasCoarse,
        shouldShowRationale = shouldShowRationale,
        permanentlyDenied = permanentlyDenied,
        requestPermissions = { launcher.launch(permissionsToRequest) },
        openAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    )
}

/**
 * Reusable Aurora Motion composable that requests Fine and Coarse location permissions
 * and displays rationale / settings recovery UI until GPS tracking is ready.
 */
@Composable
fun LocationPermissionRequester(
    modifier: Modifier = Modifier,
    permissionState: LocationPermissionState = rememberLocationPermissionState(),
    autoRequestOnFirstLaunch: Boolean = false,
    showReadyIndicator: Boolean = true,
    onPermissionReady: (hasFineLocation: Boolean, hasAnyLocation: Boolean) -> Unit = { _, _ -> },
    content: @Composable (LocationPermissionState) -> Unit = {}
) {
    val ext = LocalFitTrackColors.current

    LaunchedEffect(permissionState.hasFineLocation, permissionState.hasCoarseLocation) {
        onPermissionReady(permissionState.hasFineLocation, permissionState.hasAnyLocation)
    }

    LaunchedEffect(autoRequestOnFirstLaunch) {
        if (autoRequestOnFirstLaunch && !permissionState.hasAnyLocation) {
            permissionState.requestPermissions()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!permissionState.hasFineLocation) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentBorder = if (permissionState.hasCoarseLocation) {
                    ext.warning.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.50f)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = "GPS Location Permission",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        StatusBadge(
                            text = if (permissionState.hasCoarseLocation) {
                                "APPROXIMATE LOCATION ONLY"
                            } else {
                                "GPS PERMISSION REQUIRED"
                            },
                            tint = if (permissionState.hasCoarseLocation) ext.warning else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (permissionState.hasCoarseLocation) {
                                "Upgrade to Precise GPS Location"
                            } else {
                                "Enable Location for GPS Tracking"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                permissionState.permanentlyDenied ->
                                    "Location access was denied. Open Android App Settings to allow Fine and Coarse location for outdoor walks, runs, and rides."
                                permissionState.hasCoarseLocation ->
                                    "Approximate location is active. Grant Precise (Fine) location for accurate distance, pace splits, and route mapping."
                                else ->
                                    "FitTrack uses Fine and Coarse location only while you record outdoor walks, runs, and cycling routes."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (permissionState.permanentlyDenied) {
                    SecondaryButton(
                        text = "Open App Settings",
                        icon = Icons.Filled.Settings,
                        onClick = permissionState.openAppSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_location_settings_button")
                    )
                } else {
                    GradientButton(
                        text = if (permissionState.hasCoarseLocation) {
                            "Allow Precise GPS"
                        } else {
                            "Grant Location Access"
                        },
                        icon = Icons.Filled.LocationOn,
                        onClick = permissionState.requestPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_location_permission_button")
                    )
                }
            }
        } else if (showReadyIndicator) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Precise GPS Ready",
                    tint = ext.success,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fine & Coarse GPS location ready",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.success
                )
            }
        }

        content(permissionState)
    }
}
