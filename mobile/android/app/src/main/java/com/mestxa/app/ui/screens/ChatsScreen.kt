package com.mestxa.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.network.DirectoryEntryDto
import com.mestxa.app.network.MestxaApiClient
import com.mestxa.app.storage.CallRecordEntity
import com.mestxa.app.storage.ContactRecord
import com.mestxa.app.storage.ConversationRecord
import com.mestxa.app.storage.DatabaseManager
import com.mestxa.app.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val AvatarGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFFF4A261), Color(0xFF5A2D0C))),
    Brush.linearGradient(listOf(Color(0xFFE9C46A), Color(0xFF8A5A00))),
    Brush.linearGradient(listOf(Color(0xFF2A9D8F), Color(0xFF0B3D3A))),
    Brush.linearGradient(listOf(Color(0xFFE76F51), Color(0xFF6B1F0F))),
    Brush.linearGradient(listOf(Color(0xFF8ECAE6), Color(0xFF1D4E6B)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onOpenChat: (String, String) -> Unit,
    onStartCall: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onViewStatus: (String) -> Unit = {},
    onAddStatus: () -> Unit = {},
    onOpenKeypad: () -> Unit = {},
    onOpenScheduleCall: () -> Unit = {},
    onOpenArchivedChats: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { DatabaseManager.getInstance(context) }
    val theme = ThemeManager.colors

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Updates, 2: Communities, 3: Calls
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var selectedChatForAction by remember { mutableStateOf<ConversationRecord?>(null) }

    // Live state backed by SQLite local vault
    var conversations by remember { mutableStateOf<List<ConversationRecord>>(emptyList()) }
    var archivedConversations by remember { mutableStateOf<List<ConversationRecord>>(emptyList()) }
    var calls by remember { mutableStateOf<List<CallRecordEntity>>(emptyList()) }
    var contacts by remember { mutableStateOf<List<ContactRecord>>(emptyList()) }

    fun refreshData() {
        conversations = db.getConversations(includeArchived = false)
        archivedConversations = db.getArchivedConversations()
        calls = db.getCalls()
        contacts = db.getContacts()
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        containerColor = theme.bg,
        topBar = {
            if (isSearchActive) {
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
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", tint = theme.tx)
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = when (selectedTab) {
                                        1 -> "Search updates"
                                        2 -> "Search communities"
                                        3 -> "Name, number, @username"
                                        else -> "Search chats"
                                    },
                                    fontSize = 16.sp,
                                    color = theme.s2
                                )
                            },
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
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = theme.s2)
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                1 -> "Updates"
                                2 -> "Communities"
                                3 -> "Calls"
                                else -> "Chats"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.tx,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = theme.tx)
                        }
                        Box {
                            IconButton(onClick = { showMenuDropdown = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = theme.tx)
                            }
                            DropdownMenu(
                                expanded = showMenuDropdown,
                                onDismissRequest = { showMenuDropdown = false },
                                modifier = Modifier
                                    .background(theme.sh)
                                    .border(1.dp, theme.ol, RoundedCornerShape(16.dp))
                            ) {
                                when (selectedTab) {
                                    0 -> {
                                        DropdownMenuItem(
                                            text = { Text("New group", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                showNewChatSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("New broadcast", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                Toast.makeText(context, "Coming in future release", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Linked devices", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                Toast.makeText(context, "No linked devices", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Read all", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                db.markAllConversationsRead()
                                                refreshData()
                                                Toast.makeText(context, "Marked all as read", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                    1 -> {
                                        DropdownMenuItem(
                                            text = { Text("Status privacy", color = theme.tx, fontSize = 15.sp) },
                                            onClick = { showMenuDropdown = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                    2 -> {
                                        DropdownMenuItem(
                                            text = { Text("Settings", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                    3 -> {
                                        DropdownMenuItem(
                                            text = { Text("Clear call log", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                Toast.makeText(context, "Call log cleared", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings", color = theme.tx, fontSize = 15.sp) },
                                            onClick = {
                                                showMenuDropdown = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bg)
                )
            }
        },
        bottomBar = {
            // Prototype 80dp Tab Bar (.tb: height 80px, background var(--bg), border-top 1px solid var(--ol))
            Surface(
                color = theme.bg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Column {
                    Divider(color = theme.ol, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple(0, "Chats", Icons.Default.ChatBubble),
                            Triple(1, "Updates", Icons.Default.Schedule),
                            Triple(2, "Communities", Icons.Default.Groups),
                            Triple(3, "Calls", Icons.Default.Phone)
                        )

                        tabs.forEach { (index, title, icon) ->
                            val isSelected = selectedTab == index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedTab = index },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Prototype active indicator pill: 64x32dp, radius 16dp
                                Box(
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) theme.sfa else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (isSelected) theme.acl else theme.s2,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) theme.acl else theme.s2
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                // ================= TAB 0: CHATS =================
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Pills (.pills)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("All", "Unread", "Favorites", "Groups").forEach { filter ->
                                val isSelected = selectedFilter == filter
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (isSelected) theme.sfa else Color.Transparent)
                                        .border(1.dp, if (isSelected) theme.sfa else theme.ol, RoundedCornerShape(15.dp))
                                        .clickable { selectedFilter = filter }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter,
                                        fontSize = 13.sp,
                                        color = if (isSelected) theme.acl else theme.s2,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            // Plus Pill matching prototype .pl.pc
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, theme.ol, CircleShape)
                                    .clickable { Toast.makeText(context, "Coming in the full design", Toast.LENGTH_SHORT).show() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add filter",
                                    tint = theme.s2,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Filter by tab pills and search query
                        val filteredChats = conversations.filter { chat ->
                            val matchesFilter = when (selectedFilter) {
                                "Unread" -> chat.unreadCount > 0
                                "Favorites" -> chat.isFavorite
                                else -> true
                            }
                            val matchesQuery = searchQuery.isBlank() ||
                                chat.contactName.contains(searchQuery, ignoreCase = true) ||
                                chat.contactHandle.contains(searchQuery, ignoreCase = true) ||
                                chat.contactNumber.contains(searchQuery, ignoreCase = true) ||
                                chat.lastMessage.contains(searchQuery, ignoreCase = true)
                            matchesFilter && matchesQuery
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Archived Row
                            if (archivedConversations.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clickable { onOpenArchivedChats() }
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.width(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = "Archived",
                                                tint = theme.s2,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = "Archived",
                                            fontSize = 15.sp,
                                            color = theme.s2,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = archivedConversations.size.toString(),
                                            fontSize = 14.sp,
                                            color = theme.s2
                                        )
                                    }
                                }
                            }

                            if (filteredChats.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Nothing here yet",
                                            fontSize = 15.sp,
                                            color = theme.s2
                                        )
                                    }
                                }
                            } else {
                                items(filteredChats) { chat ->
                                    ConversationRow(
                                        chat = chat,
                                        onClick = { onOpenChat(chat.id, chat.contactName) },
                                        onLongClick = { selectedChatForAction = chat }
                                    )
                                }
                            }
                        }
                    }

                    // Floating Action Button (.fab: 56x56dp, radius 16dp, background --ac, tint --acx)
                    FloatingActionButton(
                        onClick = { showNewChatSheet = true },
                        containerColor = theme.ac,
                        contentColor = theme.acx,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .size(56.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Chat")
                    }
                }

                // ================= TAB 1: UPDATES =================
                1 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "Status",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.tx,
                                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
                            )
                        }

                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Add Status Card
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 76.dp, height = 135.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(theme.sfa)
                                            .clickable { onAddStatus() }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.sf),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Add status",
                                                    tint = theme.acl,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Add status",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = theme.tx,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Channels",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.tx
                                )
                                Button(
                                    onClick = { Toast.makeText(context, "Explore channels", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = theme.sfa,
                                        contentColor = theme.tx
                                    ),
                                    shape = RoundedCornerShape(22.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Explore", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No updates yet", color = theme.s2, fontSize = 15.sp)
                            }
                        }
                    }

                    // Updates FABs: Text status + Camera status
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { onAddStatus() },
                            containerColor = theme.sfa,
                            contentColor = theme.acl,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Text status", modifier = Modifier.size(18.dp))
                        }
                        FloatingActionButton(
                            onClick = { onAddStatus() },
                            containerColor = theme.ac,
                            contentColor = theme.acx,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera status")
                        }
                    }
                }

                // ================= TAB 2: COMMUNITIES =================
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(theme.sf)
                                .border(1.dp, theme.ol, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = theme.s2,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Communities",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.tx
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bring members together in topic-based groups and easily manage announcements.",
                            fontSize = 14.sp,
                            color = theme.s2,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    FloatingActionButton(
                        onClick = { showNewChatSheet = true },
                        containerColor = theme.ac,
                        contentColor = theme.acx,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Community")
                    }
                }

                // ================= TAB 3: CALLS =================
                3 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Quick Action Tray (.qa)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                QuickCallButton(
                                    icon = Icons.Default.Phone,
                                    label = "Call",
                                    onClick = { showNewChatSheet = true }
                                )
                                QuickCallButton(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "Schedule",
                                    onClick = { onOpenScheduleCall() }
                                )
                                QuickCallButton(
                                    icon = Icons.Default.Dialpad,
                                    label = "Keypad",
                                    onClick = { onOpenKeypad() }
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Recent",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.tx,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (calls.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No recent calls", fontSize = 14.sp, color = theme.s2)
                                }
                            }
                        } else {
                            val filteredCalls = calls.filter { call ->
                                searchQuery.isBlank() || call.contactName.contains(searchQuery, ignoreCase = true)
                            }
                            items(filteredCalls) { call ->
                                CallRecordRow(
                                    call = call,
                                    onCallClick = { onStartCall(call.contactName) }
                                )
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showNewChatSheet = true },
                        containerColor = theme.ac,
                        contentColor = theme.acx,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .size(56.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "New Call")
                    }
                }
            }
        }
    }

    // Long-Press Row Action Modal Bottom Sheet
    selectedChatForAction?.let { chat ->
        ModalBottomSheet(
            onDismissRequest = { selectedChatForAction = null },
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
                Text(
                    text = chat.contactName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.tx,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.sf)
                        .border(1.dp, theme.ol, RoundedCornerShape(16.dp))
                ) {
                    MessageActionItem(
                        icon = if (chat.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        title = if (chat.isPinned) "Unpin chat" else "Pin chat",
                        tint = theme.tx
                    ) {
                        db.setConversationPinned(chat.id, !chat.isPinned)
                        refreshData()
                        selectedChatForAction = null
                    }
                    Divider(color = theme.ol, thickness = 0.5.dp)
                    MessageActionItem(
                        icon = Icons.Default.Archive,
                        title = if (chat.isArchived) "Unarchive chat" else "Archive chat",
                        tint = theme.tx
                    ) {
                        db.setConversationArchived(chat.id, !chat.isArchived)
                        refreshData()
                        selectedChatForAction = null
                    }
                    Divider(color = theme.ol, thickness = 0.5.dp)
                    MessageActionItem(
                        icon = if (chat.isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        title = if (chat.isMuted) "Unmute notifications" else "Mute notifications",
                        tint = theme.tx
                    ) {
                        db.setConversationMuted(chat.id, !chat.isMuted)
                        refreshData()
                        selectedChatForAction = null
                    }
                    Divider(color = theme.ol, thickness = 0.5.dp)
                    MessageActionItem(
                        icon = Icons.Default.MarkEmailRead,
                        title = if (chat.unreadCount > 0) "Mark as read" else "Mark as unread",
                        tint = theme.tx
                    ) {
                        db.setConversationRead(chat.id, chat.unreadCount == 0)
                        refreshData()
                        selectedChatForAction = null
                    }
                    Divider(color = theme.ol, thickness = 0.5.dp)
                    MessageActionItem(
                        icon = Icons.Default.Delete,
                        title = "Delete chat",
                        tint = Color(0xFFEF4444)
                    ) {
                        db.deleteConversation(chat.id)
                        refreshData()
                        selectedChatForAction = null
                        Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: New Chat & Directory Lookup
    if (showNewChatSheet) {
        NewChatBottomSheet(
            contacts = contacts,
            onDismiss = { showNewChatSheet = false },
            onSelectContact = { contact ->
                val conv = ConversationRecord(
                    id = contact.userHex,
                    contactName = contact.name.ifBlank { contact.username },
                    contactHandle = contact.username,
                    contactNumber = contact.number,
                    contactPublicKeyHex = contact.userHex,
                    lastMessage = "",
                    timestamp = "Just now",
                    colorGradientIdx = (contact.username.hashCode().and(0x7fffffff)) % AvatarGradients.size
                )
                db.saveConversation(conv)
                refreshData()
                showNewChatSheet = false
                onOpenChat(conv.id, conv.contactName)
            },
            onUserFoundAndStartChat = { entry ->
                val conv = ConversationRecord(
                    id = entry.userHex,
                    contactName = entry.name.ifBlank { entry.username },
                    contactHandle = entry.username,
                    contactNumber = entry.number,
                    contactPublicKeyHex = entry.userHex,
                    lastMessage = "",
                    timestamp = "Just now",
                    colorGradientIdx = (entry.username.hashCode().and(0x7fffffff)) % AvatarGradients.size
                )
                db.saveConversation(conv)
                db.saveContact(
                    ContactRecord(
                        userHex = entry.userHex,
                        username = entry.username,
                        number = entry.number,
                        name = entry.name.ifBlank { entry.username }
                    )
                )
                refreshData()
                showNewChatSheet = false
                onOpenChat(conv.id, conv.contactName)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationRow(
    chat: ConversationRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val theme = ThemeManager.colors
    val gradient = AvatarGradients[chat.colorGradientIdx.coerceIn(0, AvatarGradients.size - 1)]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.contactName.take(2).uppercase(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.contactName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = theme.tx,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.unreadCount == 0 && chat.isDelivered) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = if (chat.isRead) theme.acl else theme.s2,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = chat.lastMessage.ifBlank { "@${chat.contactHandle}" },
                    fontSize = 15.sp,
                    color = theme.s2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = chat.timestamp,
                fontSize = 13.sp,
                color = if (chat.unreadCount > 0) theme.acl else theme.s2
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.ac)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.acx
                    )
                }
            } else if (chat.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = theme.s2,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CallRecordRow(
    call: CallRecordEntity,
    onCallClick: () -> Unit
) {
    val theme = ThemeManager.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onCallClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
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
                text = call.contactName.take(2).uppercase(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = theme.tx
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.contactName,
                fontSize = 16.sp,
                fontWeight = if (call.isMissed) FontWeight.Bold else FontWeight.Medium,
                color = if (call.isMissed) Color(0xFFEF4444) else theme.tx
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (call.isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = if (call.isMissed) Color(0xFFEF4444) else theme.acl,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = call.timestamp,
                    fontSize = 13.sp,
                    color = theme.s2
                )
            }
        }
        IconButton(onClick = onCallClick) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                contentDescription = "Call",
                tint = theme.acl
            )
        }
    }
}

@Composable
fun QuickCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val theme = ThemeManager.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(theme.sfa)
                .border(1.dp, theme.ol, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = theme.acl, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 13.sp, color = theme.s2, fontWeight = FontWeight.Normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatBottomSheet(
    contacts: List<ContactRecord>,
    onDismiss: () -> Unit,
    onSelectContact: (ContactRecord) -> Unit,
    onUserFoundAndStartChat: (DirectoryEntryDto) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { MestxaApiClient.getInstance() }
    val theme = ThemeManager.colors

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<DirectoryEntryDto?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun triggerSearch(query: String) {
        searchJob?.cancel()
        val clean = query.trim()
        if (clean.length < 3) {
            isSearching = false
            searchResult = null
            searchError = null
            return
        }

        searchJob = coroutineScope.launch {
            isSearching = true
            searchError = null
            delay(350) // debounce typing
            val result = apiClient.lookupDirectory(clean)
            isSearching = false
            if (result != null) {
                searchResult = result
                searchError = null
            } else {
                searchResult = null
                searchError = "User not found"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New chat",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = theme.tx,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Search by MX number or @username
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    triggerSearch(it)
                },
                placeholder = {
                    Text("Search @username or MX-…", color = theme.s2, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = theme.s2)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = theme.acl,
                            strokeWidth = 2.dp
                        )
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.acl,
                    unfocusedBorderColor = theme.ol,
                    focusedTextColor = theme.tx,
                    unfocusedTextColor = theme.tx
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Directory Search Result
            searchResult?.let { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onUserFoundAndStartChat(user) },
                    colors = CardDefaults.cardColors(containerColor = theme.sfa),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(theme.sf)
                                .border(1.dp, theme.ol, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user.name.ifBlank { user.username }).take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = theme.tx
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name.ifBlank { user.username },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = theme.tx
                            )
                            Text(
                                text = "@${user.username} • ${user.number}",
                                fontSize = 12.sp,
                                color = theme.s2
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = theme.acl
                        )
                    }
                }
            }

            searchError?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Contacts List
            Text(
                text = "Contacts (${contacts.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = theme.s2,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (contacts.isEmpty()) {
                Text(
                    text = "No saved contacts yet. Search an MX number or @username above to connect.",
                    fontSize = 13.sp,
                    color = theme.s2,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(contacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectContact(contact) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(theme.sf),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(2).uppercase(),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = theme.tx
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = theme.tx
                                )
                                Text(
                                    text = "@${contact.username}",
                                    fontSize = 12.sp,
                                    color = theme.s2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
