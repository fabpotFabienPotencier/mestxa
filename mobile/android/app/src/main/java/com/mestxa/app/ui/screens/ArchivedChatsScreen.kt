package com.mestxa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.storage.ConversationRecord
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit
) {
    val context = LocalContext.current
    val theme = ThemeManager.colors
    val db = remember { DatabaseManager.getInstance(context) }
    var archivedChats by remember { mutableStateOf<List<ConversationRecord>>(emptyList()) }

    fun refresh() {
        archivedChats = db.getArchivedConversations()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            TopAppBar(
                title = { Text("Archived", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.tx) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.tx)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
            )
        }
    ) { paddingValues ->
        if (archivedChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = null,
                        tint = theme.s2,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No archived chats",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.s2
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(archivedChats) { chat ->
                    ConversationRow(
                        chat = chat,
                        onClick = { onOpenChat(chat.id, chat.contactName) },
                        onLongClick = {
                            db.setConversationArchived(chat.id, false)
                            refresh()
                        }
                    )
                }
            }
        }
    }
}
