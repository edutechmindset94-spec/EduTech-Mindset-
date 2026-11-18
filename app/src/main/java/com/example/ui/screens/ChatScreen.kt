package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.CryptoManager
import com.example.core.LocalAppStrings
import com.example.data.model.AcademyMessage
import com.example.data.model.AiUiState
import com.example.data.model.SupportChatEntry
import com.example.data.model.UiState
import com.example.data.model.UserRole
import com.example.ui.theme.AcademyTeal
import com.example.ui.theme.EmeraldSuccess

@Composable
fun ChatScreen(
    currentUserId: String,
    messagesState: UiState<List<AcademyMessage>>,
    supportHistory: List<SupportChatEntry>,
    supportAiState: AiUiState,
    onSendPeerMessage: (String, Boolean) -> Unit,
    onDeletePeerMessage: (String) -> Unit,
    onAskAiSupport: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var peerDraft by remember { mutableStateOf("") }
    var supportDraft by remember { mutableStateOf("") }
    var encryptWithAes by remember { mutableStateOf(true) }

    val peerListState = rememberLazyListState()
    val supportListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("tab_peer_chat")
            ) {
                Text(strings.peerChatTab)
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("tab_ai_support")
            ) {
                Text(strings.aiSupportTab)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (selectedTab == 0) {
            // Peer & Classroom Live Chat with AES-256-GCM
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = encryptWithAes,
                    onClick = { encryptWithAes = !encryptWithAes },
                    label = { Text(strings.encryptToggleLabel) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("aes_encrypt_toggle")
                )
                Text(
                    text = if (strings.isArabic) "غرفة التعليم التعاوني" else "Collaborative Room",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                when (messagesState) {
                    is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorCard(messagesState.message) }
                    is UiState.Success -> {
                        val msgs = messagesState.data
                        LaunchedEffect(msgs.size) {
                            if (msgs.isNotEmpty()) {
                                peerListState.animateScrollToItem(msgs.lastIndex)
                            }
                        }
                        if (msgs.isEmpty()) {
                            Box(Modifier.align(Alignment.Center)) {
                                EmptyStateCard(strings.emptyChatMsg)
                            }
                        } else {
                            LazyColumn(
                                state = peerListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(msgs, key = { it.id }) { msg ->
                                    PeerMessageBubble(
                                        message = msg,
                                        isMine = msg.senderId == currentUserId,
                                        isArabic = strings.isArabic,
                                        onDelete = { onDeletePeerMessage(msg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = peerDraft,
                    onValueChange = { peerDraft = it },
                    placeholder = { Text(strings.messageHint) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("peer_chat_input"),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3
                )
                FilledIconButton(
                    onClick = {
                        if (peerDraft.isNotBlank()) {
                            onSendPeerMessage(peerDraft, encryptWithAes)
                            peerDraft = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("peer_chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.sendBtn)
                }
            }
        } else {
            // 24/7 AI Technical Support & Academic Tutor
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (strings.isArabic) {
                            "دعم فني وأكاديمي فوري 24/7 مدعوم بـ Firebase AI Logic"
                        } else {
                            "24/7 Smart Technical & Academic Support powered by Firebase AI Logic"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            LaunchedEffect(supportHistory.size, supportAiState) {
                if (supportHistory.isNotEmpty()) {
                    supportListState.animateScrollToItem(supportHistory.lastIndex)
                }
            }

            LazyColumn(
                state = supportListState,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(supportHistory, key = { it.id }) { entry ->
                    SupportBubble(isUser = entry.isUser, text = entry.text)
                }
                if (supportAiState is AiUiState.Loading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp))
                            Text(if (strings.isArabic) "يكتب الدعم الذكي..." else "Support AI is typing...")
                        }
                    }
                } else if (supportAiState is AiUiState.Streaming) {
                    item {
                        SupportBubble(isUser = false, text = supportAiState.text)
                    }
                } else if (supportAiState is AiUiState.Error) {
                    item { ErrorCard(supportAiState.message) }
                }
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = supportDraft,
                    onValueChange = { supportDraft = it },
                    placeholder = { Text(strings.aiSupportHint) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_support_input"),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3
                )
                FilledIconButton(
                    onClick = {
                        if (supportDraft.isNotBlank() && supportAiState !is AiUiState.Loading && supportAiState !is AiUiState.Streaming) {
                            onAskAiSupport(supportDraft)
                            supportDraft = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("ai_support_send_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = strings.sendBtn)
                }
            }
        }
    }
}

@Composable
private fun PeerMessageBubble(
    message: AcademyMessage,
    isMine: Boolean,
    isArabic: Boolean,
    onDelete: () -> Unit
) {
    val displayText = if (message.encrypted && message.encryptedPayload.isNotBlank()) {
        CryptoManager.decryptMessage(message.encryptedPayload, message.channelId) ?: message.body
    } else {
        message.body
    }
    val roleLabel = if (UserRole.fromRaw(message.senderRole) == UserRole.TEACHER) {
        if (isArabic) "معلم" else "Teacher"
    } else {
        if (isArabic) "طالب" else "Student"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${message.senderName} • $roleLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (message.encrypted) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "AES-256",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(text = displayText, style = MaterialTheme.typography.bodyMedium)
                if (isMine) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (isArabic) "حذف" else "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportBubble(isUser: Boolean, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) AcademyTeal else MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
