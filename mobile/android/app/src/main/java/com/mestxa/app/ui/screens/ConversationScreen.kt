package com.mestxa.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.ThemeManager
import com.mestxa.app.ui.viewmodels.ConversationViewModel
import org.json.JSONObject

data class MessageBubble(
    val id: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: String,
    val isVoiceNote: Boolean = false,
    val isRead: Boolean = true,
    val cardType: String = "text",
    val cardData: String = "",
    val quoteReply: String = "",
    val reaction: String = "",
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    contactName: String,
    conversationId: String = contactName,
    onBack: () -> Unit,
    onStartCall: (String) -> Unit,
    onOpenContactInfo: (String) -> Unit = {},
    viewModel: ConversationViewModel = remember(conversationId) { 
        ConversationViewModel(contactName = contactName, conversationId = conversationId) 
    }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val theme = ThemeManager.colors

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<MessageBubble?>(null) }
    var showDeleteSubmenu by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<MessageBubble?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val filteredMessages = remember(messages, searchQuery, searchMode) {
        if (!searchMode || searchQuery.isBlank()) {
            messages
        } else {
            messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            if (searchMode) {
                // In-Chat Search Bar
                Surface(
                    color = theme.sh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            searchMode = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", tint = theme.tx)
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search in chat", color = theme.s2, fontSize = 16.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = theme.tx,
                                unfocusedTextColor = theme.tx
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "${filteredMessages.size} found",
                                color = theme.acl,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = theme.s2)
                            }
                        }
                    }
                }
            } else {
                // Standard Prototype Conversation TopBar (.cv .hd)
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onOpenContactInfo(contactName) }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(theme.sf)
                                    .border(1.dp, theme.ol, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contactName.take(2).uppercase(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.tx
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contactName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.tx,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "online",
                                    fontSize = 12.sp,
                                    color = theme.acl
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.tx)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onStartCall(contactName) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = theme.tx)
                        }
                        IconButton(onClick = { onStartCall(contactName) }) {
                            Icon(Icons.Default.Phone, contentDescription = "Voice Call", tint = theme.tx)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = theme.tx)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .background(theme.sh)
                                    .border(1.dp, theme.ol, RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View contact", color = theme.tx, fontSize = 15.sp) },
                                    onClick = {
                                        showMenu = false
                                        onOpenContactInfo(contactName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search chat", color = theme.tx, fontSize = 15.sp) },
                                    onClick = {
                                        showMenu = false
                                        searchMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear chat", color = theme.tx, fontSize = 15.sp) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.clearChat()
                                        Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Close chat", color = theme.tx, fontSize = 15.sp) },
                                    onClick = {
                                        showMenu = false
                                        onBack()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
                )
            }
        },
        bottomBar = {
            // Floating Input Bar Container (.bar)
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column {
                    // Replying Quote Banner (#rq)
                    replyingToMessage?.let { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.sfa)
                                .border(1.dp, theme.ol, RoundedCornerShape(10.dp))
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .background(theme.acl, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reply to ${if (reply.isOutgoing) "You" else contactName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.acl
                                )
                                Text(
                                    text = reply.text.ifBlank { reply.cardType.uppercase() },
                                    fontSize = 13.sp,
                                    color = theme.s2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { replyingToMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel Reply",
                                    tint = theme.s2,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Floating Pill Input Bar (.bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // The Pill Container (.in: 52dp height, 26dp radius, background --sfa)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(theme.sfa)
                                .border(1.dp, theme.ol.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        Icons.Default.Mood,
                                        contentDescription = "Emoji",
                                        tint = theme.s2,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = "Message",
                                            fontSize = 16.sp,
                                            color = theme.s2
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = theme.tx,
                                        unfocusedTextColor = theme.tx
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = "Attach",
                                        tint = theme.s2,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.sendMessage(text = "", cardType = "img")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Camera",
                                        tint = theme.s2,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Dynamic Action Button (.snd: 52x52dp, background --ac, tint --acx)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(theme.ac)
                                .clickable {
                                    val trimmed = inputText.trim()
                                    if (trimmed.isNotBlank()) {
                                        val quote = replyingToMessage?.text ?: ""
                                        viewModel.sendMessage(text = trimmed, quoteReply = quote)
                                        inputText = ""
                                        replyingToMessage = null
                                    } else {
                                        // Mic clicked: sends Speech Note card with waveform (.card vn)
                                        viewModel.sendMessage(text = "", cardType = "vn")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (inputText.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                contentDescription = if (inputText.isNotBlank()) "Send" else "Voice Note",
                                tint = theme.acx,
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
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.sf)
                            .border(1.dp, theme.ol, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.s2
                        )
                    }
                }
            }

            itemsIndexed(filteredMessages) { index, msg ->
                val isFirstInGroup = index == 0 || filteredMessages[index - 1].isOutgoing != msg.isOutgoing
                MessageBubbleItem(
                    msg = msg,
                    isFirstInGroup = isFirstInGroup,
                    onLongClick = {
                        selectedMessageForAction = msg
                        showDeleteSubmenu = false
                    },
                    onVote = { optionIndex ->
                        viewModel.votePoll(msg.id, optionIndex)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Long-Press Message Menu Modal
    selectedMessageForAction?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = {
                selectedMessageForAction = null
                showDeleteSubmenu = false
            },
            containerColor = theme.sh,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(theme.s2, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp)
            ) {
                // Quick Emoji Reactions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(theme.sf)
                        .border(1.dp, theme.ol, RoundedCornerShape(28.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("👍🏾", "❤️", "😂", "😮", "😢", "🙏🏾").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.reactToMessage(msg.id, emoji)
                                    selectedMessageForAction = null
                                }
                                .padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showDeleteSubmenu) {
                    // Delete options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.sf)
                            .border(1.dp, theme.ol, RoundedCornerShape(16.dp))
                    ) {
                        MessageActionItem(
                            icon = Icons.Default.Delete,
                            title = "Delete for me",
                            tint = theme.tx
                        ) {
                            viewModel.deleteMessageForMe(msg.id)
                            selectedMessageForAction = null
                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                        }
                        if (msg.isOutgoing) {
                            Divider(color = theme.ol, thickness = 0.5.dp)
                            MessageActionItem(
                                icon = Icons.Default.DeleteForever,
                                title = "Delete for everyone",
                                tint = Color(0xFFEF4444)
                            ) {
                                viewModel.retractMessage(msg.id)
                                selectedMessageForAction = null
                                Toast.makeText(context, "Message deleted for everyone", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Actions Menu
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.sf)
                            .border(1.dp, theme.ol, RoundedCornerShape(16.dp))
                    ) {
                        MessageActionItem(
                            icon = Icons.Default.Reply,
                            title = "Reply",
                            tint = theme.tx
                        ) {
                            replyingToMessage = msg
                            selectedMessageForAction = null
                        }
                        Divider(color = theme.ol, thickness = 0.5.dp)
                        MessageActionItem(
                            icon = Icons.Default.ContentCopy,
                            title = "Copy",
                            tint = theme.tx
                        ) {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            selectedMessageForAction = null
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        Divider(color = theme.ol, thickness = 0.5.dp)
                        MessageActionItem(
                            icon = Icons.Default.Forward,
                            title = "Forward",
                            tint = theme.tx
                        ) {
                            selectedMessageForAction = null
                            Toast.makeText(context, "Select a contact to forward", Toast.LENGTH_SHORT).show()
                        }
                        Divider(color = theme.ol, thickness = 0.5.dp)
                        MessageActionItem(
                            icon = if (msg.isStarred) Icons.Default.StarOutline else Icons.Default.Star,
                            title = if (msg.isStarred) "Unstar" else "Star",
                            tint = theme.tx
                        ) {
                            viewModel.toggleStar(msg.id)
                            selectedMessageForAction = null
                            Toast.makeText(context, if (msg.isStarred) "Unstarred" else "Starred", Toast.LENGTH_SHORT).show()
                        }
                        Divider(color = theme.ol, thickness = 0.5.dp)
                        MessageActionItem(
                            icon = Icons.Default.Delete,
                            title = "Delete",
                            tint = Color(0xFFEF4444)
                        ) {
                            showDeleteSubmenu = true
                        }
                    }
                }
            }
        }
    }

    // Attachment Modal Sheet (#sh)
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = theme.sh,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(theme.s2, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // 4x2 Grid of actions matching prototype
                val actions = listOf(
                    Triple("Gallery", Icons.Default.Image, "img"),
                    Triple("Camera", Icons.Default.PhotoCamera, "img"),
                    Triple("Document", Icons.Default.Description, "doc"),
                    Triple("Location", Icons.Default.LocationOn, "loc"),
                    Triple("Poll", Icons.Default.Poll, "poll"),
                    Triple("Event", Icons.Default.Event, "evt"),
                    Triple("Contact", Icons.Default.Person, "con"),
                    Triple("Audio", Icons.Default.Headphones, "vn")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    actions.take(4).forEach { (title, icon, type) ->
                        AttachmentPillButton(title = title, icon = icon) {
                            showAttachmentSheet = false
                            when (type) {
                                "loc" -> viewModel.sendMessage(text = "", cardType = "loc", cardData = "Victoria Island, Lagos")
                                "doc" -> viewModel.sendMessage(text = "", cardType = "doc", cardData = "{\"title\":\"Document.pdf\",\"size\":\"1.2 MB\"}")
                                else -> viewModel.sendMessage(text = "", cardType = type)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    actions.drop(4).forEach { (title, icon, type) ->
                        AttachmentPillButton(title = title, icon = icon) {
                            showAttachmentSheet = false
                            when (type) {
                                "poll" -> viewModel.sendMessage(
                                    text = "",
                                    cardType = "poll",
                                    cardData = "{\"question\":\"Team sync tomorrow?\",\"options\":[\"Yes, morning\",\"Afternoon\"],\"votes\":[0,0]}"
                                )
                                "evt" -> viewModel.sendMessage(
                                    text = "",
                                    cardType = "evt",
                                    cardData = "{\"title\":\"Product Review\",\"time\":\"Tomorrow, 10:00 AM\"}"
                                )
                                "con" -> viewModel.sendMessage(
                                    text = "",
                                    cardType = "con",
                                    cardData = "{\"name\":\"$contactName\",\"number\":\"Mestxa Verified\"}"
                                )
                                "vn" -> viewModel.sendMessage(
                                    text = "",
                                    cardType = "vn",
                                    cardData = "{\"duration\":\"0:24\"}"
                                )
                                else -> viewModel.sendMessage(text = "", cardType = type)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Prototype .ph 4-color photo preview strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val colors = listOf(
                        Brush.linearGradient(listOf(Color(0xFF2A9D8F), Color(0xFF0B3D3A))),
                        Brush.linearGradient(listOf(Color(0xFFE76F51), Color(0xFF6B1F0F))),
                        Brush.linearGradient(listOf(Color(0xFF8ECAE6), Color(0xFF1D4E6B))),
                        Brush.linearGradient(listOf(Color(0xFFF4A261), Color(0xFF5A2D0C)))
                    )
                    colors.forEach { brush ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(brush)
                                .clickable {
                                    showAttachmentSheet = false
                                    viewModel.sendMessage(text = "", cardType = "img")
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    msg: MessageBubble,
    isFirstInGroup: Boolean,
    onLongClick: () -> Unit,
    onVote: (Int) -> Unit
) {
    val theme = ThemeManager.colors

    // Bubble Shape: 12dp all corners, except top corner when first in group: 3dp
    val bubbleShape = RoundedCornerShape(
        topStart = if (!msg.isOutgoing && isFirstInGroup) 3.dp else 12.dp,
        topEnd = if (msg.isOutgoing && isFirstInGroup) 3.dp else 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )

    val bubbleBg = if (msg.isOutgoing) theme.ac else theme.sfa
    val textColor = if (msg.isOutgoing) theme.acx else theme.tx

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (msg.isOutgoing) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(bubbleShape)
                    .background(bubbleBg)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    // Quoted message reply banner (.qt)
                    if (msg.quoteReply.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(textColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(26.dp)
                                    .background(textColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg.quoteReply,
                                fontSize = 13.sp,
                                color = textColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Deleted state
                    if (msg.isDeleted) {
                        Text(
                            text = "This message was deleted",
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    } else {
                        // Rich Card Payload Rendering
                        when (msg.cardType) {
                            "img" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2A9D8F), Color(0xFF0B3D3A))
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            "doc" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(textColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = "Doc",
                                            tint = textColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Project brief.pdf",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = "1.2 MB",
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            "loc" -> {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF222222)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = "Location",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.cardData.ifBlank { "Shared location" },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                }
                            }
                            "con" -> {
                                val (cName, cSub) = remember(msg.cardData) {
                                    try {
                                        val json = org.json.JSONObject(msg.cardData)
                                        json.optString("name", "Contact") to json.optString("number", "Mobile")
                                    } catch (e: Exception) {
                                        if (msg.cardData.contains("•")) {
                                            val parts = msg.cardData.split("•")
                                            parts[0].trim() to (parts.getOrNull(1)?.trim() ?: "Contact")
                                        } else {
                                            (msg.cardData.ifBlank { "Contact" }) to "Contact"
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(textColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cName.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cName,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = cSub,
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            "evt" -> {
                                val (eTitle, eTime) = remember(msg.cardData) {
                                    try {
                                        val json = org.json.JSONObject(msg.cardData)
                                        json.optString("title", "Event") to json.optString("time", "Scheduled")
                                    } catch (e: Exception) {
                                        if (msg.cardData.contains("|")) {
                                            val parts = msg.cardData.split("|")
                                            parts[0].trim() to (parts.getOrNull(1)?.trim() ?: "Scheduled")
                                        } else {
                                            (msg.cardData.ifBlank { "Event" }) to "Scheduled"
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(textColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Event,
                                            contentDescription = "Event",
                                            tint = textColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = eTitle,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = eTime,
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            "vn" -> {
                                // 18-bar waveform Speech Note card
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(textColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = bubbleBg,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val waveformBars = remember(msg.id) {
                                            val seed = (msg.id.hashCode() and 0x7fffffff)
                                            val list = mutableListOf<Int>()
                                            var curr = seed
                                            for (k in 0 until 18) {
                                                curr = (curr * 1103515245 + 12345) and 0x7fffffff
                                                list.add(6 + (curr % 18))
                                            }
                                            list
                                        }
                                        waveformBars.forEach { h ->
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(h.dp)
                                                    .background(
                                                        textColor.copy(alpha = 0.55f),
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val audioDuration = remember(msg.cardData) {
                                        try {
                                            val json = JSONObject(msg.cardData)
                                            json.optString("duration", "0:12")
                                        } catch (e: Exception) {
                                            "0:12"
                                        }
                                    }
                                    Text(
                                        text = audioDuration,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            "poll" -> {
                                var question = "Lunch today?"
                                var options = listOf("Jollof rice", "Pizza")
                                var votes = listOf(0, 0)
                                var myVote = -1
                                try {
                                    if (msg.cardData.isNotEmpty()) {
                                        val json = JSONObject(msg.cardData)
                                        question = json.optString("question", question)
                                        val optArr = json.optJSONArray("options")
                                        if (optArr != null) {
                                            options = (0 until optArr.length()).map { optArr.getString(it) }
                                        }
                                        val vArr = json.optJSONArray("votes")
                                        if (vArr != null) {
                                            votes = (0 until vArr.length()).map { vArr.getInt(it) }
                                        }
                                        myVote = json.optInt("myVote", -1)
                                    }
                                } catch (_: Exception) {}

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = question,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    options.forEachIndexed { idx, opt ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onVote(idx) }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = opt,
                                                fontSize = 14.sp,
                                                fontWeight = if (myVote == idx) FontWeight.Bold else FontWeight.Normal,
                                                color = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${votes.getOrElse(idx) { 0 }}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (idx < options.size - 1) {
                                            Divider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }

                        // Text content (if present)
                        if (msg.text.isNotBlank()) {
                            Text(
                                text = msg.text,
                                fontSize = 16.sp,
                                lineHeight = 21.sp,
                                color = textColor
                            )
                        }
                    }

                    // Timestamp + Status
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (msg.isStarred) {
                            Text(
                                text = "★ ",
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = msg.timestamp,
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                        if (msg.isOutgoing) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Reaction Badge (.rx)
            if (msg.reaction.isNotBlank() && !msg.isDeleted) {
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-6).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.sh)
                        .border(1.dp, theme.ol, RoundedCornerShape(10.dp))
                        .clickable { onLongClick() }
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(text = msg.reaction, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AttachmentPillButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val theme = ThemeManager.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.bg)
                .border(1.dp, theme.ol, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = theme.acl,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = theme.s2
        )
    }
}

@Composable
fun MessageActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
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
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
