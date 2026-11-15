package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiChatMessage
import com.example.ui.AsyncState
import com.example.ui.branding.FitTrackIllustrationType
import com.example.ui.components.AuroraAmbientBackground
import com.example.ui.components.ConfirmationModal
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalFitTrackColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    messages: List<AiChatMessage>,
    coachState: AsyncState<String>,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit = {},
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var lastSentPrompt by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isBusy = coachState is AsyncState.Loading || coachState is AsyncState.Streaming

    val quickPrompts = listOf(
        "Review my daily nutrition & steps",
        "Suggest a 20-min recovery routine",
        "How can I boost daily protein?",
        "Give me 5km pacing tips"
    )

    LaunchedEffect(messages.size, coachState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                title = { Text("AI Fitness Coach") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("clear_ai_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear chat history"
                            )
                        }
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
                    .imePadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(MaterialTheme.shapes.large)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ai_coach_banner_1794739037230),
                                contentDescription = "AI Fitness Coach Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                StatusBadge(text = "AURORA COACH", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Personalized Coaching", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Context-aware guidance from your steps, meals & goals",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        DisclaimerBanner(text = stringResource(R.string.ai_disclaimer))
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickPrompts.forEach { prompt ->
                                AssistChip(
                                    onClick = {
                                        lastSentPrompt = prompt
                                        onSendMessage(prompt)
                                    },
                                    enabled = !isBusy,
                                    label = { Text(prompt) }
                                )
                            }
                        }
                    }

                    if (messages.isEmpty() && coachState is AsyncState.Idle) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.AutoAwesome,
                                title = "Ask your FitTrack Coach",
                                subtitle = "Get tailored workout tips, macro strategies, or pacing guidance.",
                                illustration = FitTrackIllustrationType.AI_COACH
                            )
                        }
                    } else {
                        items(messages, key = { it.id.ifBlank { it.text.hashCode().toString() } }) { msg ->
                            val isUser = msg.role == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        LocalFitTrackColors.current.surfaceElevated
                                    },
                                    modifier = Modifier.fillMaxWidth(0.86f)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    when (coachState) {
                        is AsyncState.Loading -> {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("Coach is thinking...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is AsyncState.Streaming -> {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = LocalFitTrackColors.current.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth(0.86f)
                                ) {
                                    Text(
                                        text = coachState.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                        is AsyncState.Error -> {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ErrorBannerCard(message = coachState.message)
                                    if (lastSentPrompt.isNotBlank()) {
                                        SecondaryButton(
                                            text = "Retry Message",
                                            icon = Icons.Filled.Refresh,
                                            onClick = { onSendMessage(lastSentPrompt) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("retry_ai_coach_button")
                                        )
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask about training, macros, recovery...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_coach_input")
                        )
                        IconButton(
                            onClick = {
                                val prompt = inputText.trim()
                                if (prompt.isNotBlank()) {
                                    lastSentPrompt = prompt
                                    onSendMessage(prompt)
                                    inputText = ""
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(LocalFitTrackColors.current.auroraBrush)
                                .testTag("ai_coach_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmationModal(
            title = "Clear AI Coach History?",
            message = "This will permanently remove your saved AI Coach conversation messages.",
            confirmText = "Clear History",
            isDestructive = true,
            onConfirm = {
                showClearDialog = false
                onClearHistory()
            },
            onDismiss = { showClearDialog = false }
        )
    }
}
