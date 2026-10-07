package com.mestxa.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*
import com.mestxa.app.ui.viewmodels.ConversationViewModel

data class MessageBubble(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: String,
    val isVoiceNote: Boolean = false,
    val isRead: Boolean = true
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    contactName: String,
    onBack: () -> Unit,
    onStartCall: (String) -> Unit,
    onOpenContactInfo: (String) -> Unit = {},
    viewModel: ConversationViewModel = remember(contactName) { ConversationViewModel(contactName) }
) {
    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<MessageBubble?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageBubble?>(null) }
    var starredMessages by remember { mutableStateOf(setOf<String>()) }
    var reactionsMap by remember { mutableStateOf(mapOf<String, String>()) }

    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenContactInfo(contactName) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(SurfaceDark, CircleShape)
                                .border(1.dp, BorderHairline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contactName.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = contactName,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "• online",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onStartCall(contactName) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = TextPrimary)
                    }
                    IconButton(onClick = { onStartCall(contactName) }) {
                        Icon(Icons.Default.Phone, contentDescription = "Voice Call", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        },
        bottomBar = {
            // Chat Input Bar
            Surface(
                color = OledBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column {
                    // Replying Quote Banner
                    replyingToMessage?.let { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(34.dp)
                                    .background(AccentGreen, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (reply.isOutgoing) "You" else contactName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                                Text(
                                    text = reply.text,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { replyingToMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel Reply",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(SurfaceDark, RoundedCornerShape(24.dp))
                                .border(1.dp, BorderHairline, RoundedCornerShape(24.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.Mood,
                                        contentDescription = "Emoji",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = "Message",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = "Attach",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Camera",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Dynamic Send / Mic Action Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AccentWhite)
                                .clickable {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                        replyingToMessage = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (inputText.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                contentDescription = "Send",
                                tint = OledBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(messages) { msg ->
                MessageBubbleRow(
                    msg = msg,
                    reaction = reactionsMap[msg.id],
                    isStarred = starredMessages.contains(msg.id),
                    onLongClick = { selectedMessageForAction = msg }
                )
            }
        }
    }

    // Message Long-Press Actions & Reaction Modal
    selectedMessageForAction?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForAction = null },
            containerColor = SurfaceDark,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(BorderHairline, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
            ) {
                // Quick Emoji Reactions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderHairline, RoundedCornerShape(32.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    reactionsMap = if (reactionsMap[msg.id] == emoji) {
                                        reactionsMap - msg.id
                                    } else {
                                        reactionsMap + (msg.id to emoji)
                                    }
                                    selectedMessageForAction = null
                                }
                                .padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions Menu
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    MessageActionItem(icon = Icons.Default.Reply, title = "Reply") {
                        replyingToMessage = msg
                        selectedMessageForAction = null
                    }
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    MessageActionItem(icon = Icons.Default.ContentCopy, title = "Copy Text") {
                        selectedMessageForAction = null
                    }
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    MessageActionItem(
                        icon = if (starredMessages.contains(msg.id)) Icons.Default.StarOutline else Icons.Default.Star,
                        title = if (starredMessages.contains(msg.id)) "Unstar" else "Star"
                    ) {
                        starredMessages = if (starredMessages.contains(msg.id)) {
                            starredMessages - msg.id
                        } else {
                            starredMessages + msg.id
                        }
                        selectedMessageForAction = null
                    }
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    MessageActionItem(icon = Icons.Default.Forward, title = "Forward") {
                        selectedMessageForAction = null
                    }
                    Divider(color = BorderHairline, thickness = 0.5.dp)
                    MessageActionItem(icon = Icons.Default.Delete, title = "Delete message", isDestructive = true) {
                        viewModel.retractMessage(msg.id)
                        selectedMessageForAction = null
                    }
                }
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = SurfaceElevated,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(TextSecondary, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                val attachments = listOf(
                    Triple("Gallery", Icons.Default.Image, Color(0xFF9333EA)),
                    Triple("Camera", Icons.Default.PhotoCamera, Color(0xFFEF4444)),
                    Triple("Document", Icons.Default.Description, Color(0xFF3B82F6)),
                    Triple("Location", Icons.Default.LocationOn, Color(0xFF10B981)),
                    Triple("Poll", Icons.Default.Poll, Color(0xFFF59E0B)),
                    Triple("Contact", Icons.Default.Person, Color(0xFF06B6D4)),
                    Triple("Audio", Icons.Default.Headphones, Color(0xFFEC4899)),
                    Triple("Event", Icons.Default.Event, Color(0xFF8B5CF6))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    attachments.take(4).forEach { (title, icon, color) ->
                        AttachmentIcon(title = title, icon = icon, bg = color) {
                            showAttachmentSheet = false
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    attachments.drop(4).forEach { (title, icon, color) ->
                        AttachmentIcon(title = title, icon = icon, bg = color) {
                            showAttachmentSheet = false
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    msg: MessageBubble,
    reaction: String? = null,
    isStarred: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (msg.isOutgoing) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (msg.isOutgoing) 18.dp else 4.dp,
                            bottomEnd = if (msg.isOutgoing) 4.dp else 18.dp
                        )
                    )
                    .background(
                        color = if (msg.isOutgoing) BubbleOutgoing else BubbleIncoming
                    )
                    .border(
                        width = if (msg.isOutgoing) 0.dp else 1.dp,
                        color = if (msg.isOutgoing) Color.Transparent else BorderHairline,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (msg.isOutgoing) 18.dp else 4.dp,
                            bottomEnd = if (msg.isOutgoing) 4.dp else 18.dp
                        )
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                if (msg.isVoiceNote) {
                    // Playable Voice Note Layout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (msg.isOutgoing) OledBlack else AccentWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (msg.isOutgoing) AccentWhite else OledBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        // Audio waveform visualization
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            listOf(8, 16, 24, 18, 10, 28, 20, 14, 22, 12, 6).forEach { height ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(height.dp)
                                        .background(
                                            if (msg.isOutgoing) OledBlack.copy(alpha = 0.5f) else TextSecondary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.isOutgoing) OledBlack else TextSecondary
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (msg.isOutgoing) BubbleOutgoingText else BubbleIncomingText,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isStarred) {
                                Text(
                                    text = "★ ",
                                    fontSize = 11.sp,
                                    color = if (msg.isOutgoing) OledBlack else AccentGreen
                                )
                            }
                            Text(
                                text = msg.timestamp,
                                fontSize = 11.sp,
                                color = if (msg.isOutgoing) OledBlack.copy(alpha = 0.6f) else TextSecondary
                            )
                        }
                    }
                }
            }

            // Reaction pill below bubble
            reaction?.let { r ->
                Box(
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = r, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MessageActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) AccentRed else TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) AccentRed else TextPrimary
        )
    }
}

@Composable
fun AttachmentIcon(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SurfaceDark, CircleShape)
                .border(1.dp, BorderHairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AccentWhite,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
