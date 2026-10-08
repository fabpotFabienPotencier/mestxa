package com.mestxa.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.mestxa.app.ui.theme.*
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
    onAddStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { DatabaseManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Updates, 2: Communities, 3: Calls
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var viewingArchivedOnly by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        Scaffold(
            containerColor = OledBlack,
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
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
                                        style = MaterialTheme.typography.bodyLarge,
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close Search", tint = TextPrimary)
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    1 -> "Updates"
                                    2 -> "Communities"
                                    3 -> "Calls"
                                    else -> if (viewingArchivedOnly) "Archived" else "Chats"
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimary
                            )
                        },
                        navigationIcon = {
                            if (viewingArchivedOnly) {
                                IconButton(onClick = { viewingArchivedOnly = false }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                            }
                            Box {
                                IconButton(onClick = { showMenuDropdown = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextPrimary)
                                }
                                DropdownMenu(
                                    expanded = showMenuDropdown,
                                    onDismissRequest = { showMenuDropdown = false },
                                    modifier = Modifier
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New group", color = TextPrimary) },
                                        onClick = {
                                            showMenuDropdown = false
                                            showNewChatSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Linked devices", color = TextPrimary) },
                                        onClick = { showMenuDropdown = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Starred messages", color = TextPrimary) },
                                        onClick = { showMenuDropdown = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings", color = TextPrimary) },
                                        onClick = {
                                            showMenuDropdown = false
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    // ================= TAB 0: CHATS =================
                    0 -> {
                        // Filter Pills: All, Unread, Favorites, Groups
                        if (!viewingArchivedOnly) {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filters = listOf("All", "Unread", "Favorites", "Groups")
                                items(filters) { filter ->
                                    val isSelected = selectedFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(if (isSelected) AccentWhite else Color.Transparent)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) AccentWhite else BorderHairline,
                                                shape = RoundedCornerShape(15.dp)
                                            )
                                            .clickable { selectedFilter = filter }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = filter,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) OledBlack else TextSecondary
                                        )
                                    }
                                }

                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, BorderHairline, CircleShape)
                                            .clickable { showNewChatSheet = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "New chat",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val activeList = if (viewingArchivedOnly) archivedConversations else conversations

                        // Filter by tab pills and search query
                        val filteredChats = activeList.filter { chat ->
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

                        if (filteredChats.isEmpty()) {
                            // Prototype authentic empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceDark)
                                            .border(1.dp, BorderHairline, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = if (viewingArchivedOnly) "No archived chats" else "Nothing here yet",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (viewingArchivedOnly) {
                                            "Archived chats will appear here"
                                        } else {
                                            "Start a conversation by searching for someone using their MX number or @username"
                                        },
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    if (!viewingArchivedOnly) {
                                        Spacer(modifier = Modifier.height(22.dp))
                                        Button(
                                            onClick = { showNewChatSheet = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentWhite,
                                                contentColor = OledBlack
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Start a chat",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                // Archived Row (matching prototype .row style)
                                if (!viewingArchivedOnly && archivedConversations.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .clickable { viewingArchivedOnly = true }
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
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = "Archived",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = TextSecondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = archivedConversations.size.toString(),
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                items(filteredChats) { chat ->
                                    ConversationRow(
                                        chat = chat,
                                        onClick = { onOpenChat(chat.id, chat.contactName) }
                                    )
                                }
                            }
                        }
                    }

                    // ================= TAB 1: UPDATES =================
                    1 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            item {
                                Text(
                                    text = "Status",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 12.dp)
                                )
                            }

                            // Stories Tray matching prototype
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // My Status
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 86.dp, height = 138.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                                                .clickable { onAddStatus() }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF262626)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Me",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .align(Alignment.BottomEnd)
                                                            .clip(CircleShape)
                                                            .background(AccentWhite),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Add,
                                                            contentDescription = null,
                                                            tint = OledBlack,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(28.dp))
                                                Text(
                                                    text = "Add status",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Channels Section
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Channels",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    TextButton(
                                        onClick = {},
                                        colors = ButtonDefaults.textButtonColors(contentColor = AccentWhite)
                                    ) {
                                        Text(text = "Explore", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Stay updated on topics you care about",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // ================= TAB 2: COMMUNITIES =================
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderHairline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Communities",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Bring members together in topic-based groups and broadcast secure announcements with MLS TreeKEM encryption.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // ================= TAB 3: CALLS =================
                    3 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            // Quick Action Tray (.qa in prototype)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    QuickCallButton(
                                        icon = Icons.Default.Phone,
                                        label = "Call",
                                        onClick = { showNewChatSheet = true }
                                    )
                                    QuickCallButton(
                                        icon = Icons.Default.CalendarToday,
                                        label = "Schedule",
                                        onClick = {}
                                    )
                                    QuickCallButton(
                                        icon = Icons.Default.Dialpad,
                                        label = "Keypad",
                                        onClick = {}
                                    )
                                }
                            }

                            item {
                                Text(
                                    text = "Recent",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
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
                                        Text(
                                            text = "No recent calls",
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        )
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
                    }
                }
            }
        }

        // Prototype Floating Action Button (.fab: bottom 96px, right 16px, 56px size, 16px radius)
        when (selectedTab) {
            0 -> {
                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = AccentWhite,
                    contentColor = OledBlack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 16.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New Chat")
                }
            }
            1 -> {
                FloatingActionButton(
                    onClick = { onAddStatus() },
                    containerColor = AccentWhite,
                    contentColor = OledBlack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 16.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                }
            }
            2 -> {
                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = AccentWhite,
                    contentColor = OledBlack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 16.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Community")
                }
            }
            3 -> {
                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = AccentWhite,
                    contentColor = OledBlack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 16.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.AddIcCall, contentDescription = "New Call")
                }
            }
        }

        // Prototype Floating Glassmorphic Tab Bar (.tb: bottom 12px, margin 12px, height 66px, radius 33px)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(Color(0xE614181E))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(33.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple(0, "Chats", Icons.Default.Chat),
                    Triple(1, "Updates", Icons.Default.Schedule),
                    Triple(2, "Communities", Icons.Default.Groups),
                    Triple(3, "Calls", Icons.Default.Phone)
                )
                tabs.forEach { (index, title, icon) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(27.dp))
                            .background(if (isSelected) Color(0x2EFFFFFF) else Color.Transparent)
                            .clickable {
                                selectedTab = index
                                viewingArchivedOnly = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) AccentWhite else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AccentWhite else TextSecondary
                            )
                        }
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
                // Ensure conversation exists in DB
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

@Composable
fun ConversationRow(
    chat: ConversationRecord,
    onClick: () -> Unit
) {
    val gradient = AvatarGradients[chat.colorGradientIdx.coerceIn(0, AvatarGradients.size - 1)]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar matching prototype .av style
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
                fontWeight = FontWeight.Bold,
                color = AccentWhite
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.contactName,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = chat.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 13.sp,
                    color = if (chat.unreadCount > 0) AccentWhite else TextSecondary,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.unreadCount == 0 && chat.isDelivered) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = if (chat.isRead) AccentBlue else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = chat.lastMessage.ifBlank { "@${chat.contactHandle}" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(AccentWhite, CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OledBlack
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallRecordRow(
    call: CallRecordEntity,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(SurfaceDark, CircleShape)
                .border(1.dp, BorderHairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = call.contactName.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentWhite
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.contactName,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 16.sp,
                fontWeight = if (call.isMissed) FontWeight.Bold else FontWeight.Medium,
                color = if (call.isMissed) AccentRed else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (call.isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = if (call.isMissed) AccentRed else AccentGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${call.timestamp} • 48 kHz Opus",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        IconButton(onClick = onCallClick) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                contentDescription = "Call",
                tint = TextPrimary
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, BorderHairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = AccentWhite, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
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
            val entry = apiClient.lookupDirectory(clean)
            isSearching = false
            if (entry != null) {
                searchResult = entry
                searchError = null
            } else {
                searchResult = null
                searchError = "No account found for \"$clean\""
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New chat",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Directory Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    triggerSearch(it)
                },
                placeholder = {
                    Text(
                        text = "Search MX-number or @username",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = AccentWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchResult = null
                            searchError = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentWhite,
                    unfocusedBorderColor = BorderHairline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = OledBlack,
                    unfocusedContainerColor = OledBlack
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Result Card
            searchResult?.let { found ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderHairline, AccentWhite.copy(alpha = 0.3f)))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = found.name.take(2).uppercase().ifBlank { found.username.take(2).uppercase() },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = found.name.ifBlank { "@${found.username}" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "@${found.username} • ${found.number}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { onUserFoundAndStartChat(found) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentWhite,
                                contentColor = OledBlack
                            ),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            searchError?.let { err ->
                Text(
                    text = err,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Quick actions matching prototype (.row with icons)
            if (searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {}
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = AccentWhite, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "New group", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {}
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AccentWhite, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "New contact", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {}
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = AccentWhite, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "New community", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Saved Contacts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (contacts.isEmpty()) {
                    Text(
                        text = "No saved contacts yet. Enter an MX number or @username above to start messaging.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(contacts) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectContact(c) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = c.name.take(2).uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentWhite
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = c.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "@${c.username} • ${c.number}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
