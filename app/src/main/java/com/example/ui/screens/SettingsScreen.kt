package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.ConfirmationModal
import com.example.ui.components.FitTrackCard
import com.example.ui.components.GradientButton
import com.example.ui.components.SecondaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: UserProfile?,
    onChangeTheme: (String) -> Unit,
    onChangeUnits: (String) -> Unit,
    onUpdateConsent: (Boolean, Boolean) -> Unit,
    onExportJson: suspend () -> String,
    onSignOut: () -> Unit,
    onDeleteAccountData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportedPreview by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val sensorConsent = profile?.sensorConsentGranted ?: true
    val locationConsent = profile?.locationConsentGranted ?: true

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("Settings, Privacy & Data") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AuroraAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Appearance (Aurora Motion)", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("dark" to "Dark (#080B14)", "light" to "Light (#F6F8FC)", "system" to "System").forEach { (key, label) ->
                            FilterChip(
                                selected = (profile?.themePreference ?: "dark") == key,
                                onClick = { onChangeTheme(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Measurement Units", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("metric" to "Metric (kg, cm, km)", "imperial" to "Imperial (lb, in, mi)").forEach { (key, label) ->
                            FilterChip(
                                selected = (profile?.unitSystem ?: "metric") == key,
                                onClick = { onChangeUnits(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Privacy & Telemetry Controls", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Step Sensor Sync", style = MaterialTheme.typography.titleMedium)
                            Text("Count and sync daily hardware step telemetry", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = sensorConsent, onCheckedChange = { onUpdateConsent(it, locationConsent) })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GPS Location Sessions", style = MaterialTheme.typography.titleMedium)
                            Text("Record route coordinates during active workouts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = locationConsent, onCheckedChange = { onUpdateConsent(sensorConsent, it) })
                    }
                }

                FitTrackCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Data Export & Ownership", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    GradientButton(
                        text = "Export All Data (JSON)",
                        icon = Icons.Filled.Download,
                        loading = isExporting,
                        onClick = {
                            isExporting = true
                            scope.launch {
                                runCatching { onExportJson() }
                                    .onSuccess { json ->
                                        exportedPreview = json
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "FitTrack Data Export")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export FitTrack JSON"))
                                    }
                                isExporting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_data_button")
                    )
                    if (exportedPreview != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportedPreview!!.take(300) + if (exportedPreview!!.length > 300) "..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = "Sign Out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_data_button")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All User Data")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmationModal(
            title = "Delete All FitTrack Data?",
            message = "This permanently deletes your profile, step history, GPS routes, meals, workouts, weight records, and AI chat history from your account. This cannot be undone.",
            confirmText = "Delete Permanently",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccountData()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
