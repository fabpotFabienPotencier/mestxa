package com.mestxa.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mestxa.app.ui.theme.*

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isDelivered: Boolean = true,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false
)

data class CallRecord(
    val id: String,
    val name: String,
    val isOutgoing: Boolean,
    val isMissed: Boolean,
    val timestamp: String,
    val isVideo: Boolean = false
)

@Composable
fun ChatsScreen(
    onOpenChat: (String, String) -> Unit,
    onStartCall: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onViewStatus: (String) -> Unit = {},
    onAddStatus: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Updates, 2: Communities, 3: Calls
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenuDropdown by remember { mutableStateOf(false) }

    val sampleChats = remember {
        listOf(
            ChatItem("1", "Amaka", "Sent the presentation deck over", "1:10 PM", unreadCount = 1, isRead = true, isFavorite = true),
            ChatItem("2", "Chidi", "Are you reaching the office early?", "11:45 AM", unreadCount = 2, isRead = false, isFavorite = true),
            ChatItem("3", "Tunde", "I'll pick up the equipment on Monday", "9:20 AM", isDelivered = true),
            ChatItem("4", "Ngozi", "The prototypes look very clean and responsive", "Yesterday", isRead = true),
            ChatItem("5", "Emeka", "Catch you at the product sync", "Yesterday", isDelivered = true),
            ChatItem("6", "Joshua", "Final contract signed. Thank you!", "Oct 2", isRead = true, isFavorite = true)
        )
    }

    val sampleCalls = remember {
        listOf(
            CallRecord("1", "Chidi", isOutgoing = true, isMissed = false, timestamp = "Today, 2:10 PM", isVideo = false),
            CallRecord("2", "Amaka", isOutgoing = false, isMissed = false, timestamp = "Today, 1:02 PM", isVideo = true),
            CallRecord("3", "Tunde", isOutgoing = false, isMissed = true, timestamp = "Yesterday, 7:39 PM", isVideo = false),
            CallRecord("4", "Ngozi", isOutgoing = true, isMissed = false, timestamp = "Oct 2, 9:06 PM", isVideo = false)
        )
    }

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
                                else -> "Chats"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                                    onClick = { showMenuDropdown = false }
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OledBlack
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = OledBlack,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(width = 0.5.dp, color = BorderHairline)
                    .height(68.dp)
            ) {
                val navItems = listOf(
                    Triple(0, "Chats", Icons.Default.Chat),
                    Triple(1, "Updates", Icons.Default.Circle),
                    Triple(2, "Communities", Icons.Default.Groups),
                    Triple(3, "Calls", Icons.Default.Phone)
                )

                navItems.forEach { (index, title, icon) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentWhite,
                            selectedTextColor = AccentWhite,
                            indicatorColor = SurfaceDark,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = {},
                        containerColor = AccentWhite,
                        contentColor = OledBlack,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "New Chat")
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = {},
                        containerColor = AccentWhite,
                        contentColor = OledBlack,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Community")
                    }
                }
                3 -> {
                    FloatingActionButton(
                        onClick = { onStartCall("Amaka") },
                        containerColor = AccentWhite,
                        contentColor = OledBlack,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.AddIcCall, contentDescription = "New Call")
                    }
                }
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
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Unread", "Favorites", "Groups")
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) AccentWhite else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentWhite else BorderHairline,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) OledBlack else TextSecondary
                                )
                            }
                        }
                    }

                    // Conversations List
                    val filteredChats = sampleChats.filter { chat ->
                        val matchesFilter = when (selectedFilter) {
                            "Unread" -> chat.unreadCount > 0
                            "Favorites" -> chat.isFavorite
                            else -> true
                        }
                        val matchesQuery = searchQuery.isBlank() ||
                            chat.name.contains(searchQuery, ignoreCase = true) ||
                            chat.lastMessage.contains(searchQuery, ignoreCase = true)
                        matchesFilter && matchesQuery
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Archived Chats Row matching prototype
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Archived filter */ }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archived",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Archived",
                                    fontSize = 15.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "2",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        items(filteredChats) { chat ->
                            ChatRow(
                                chat = chat,
                                onClick = { onOpenChat(chat.id, chat.name) }
                            )
                        }
                    }
                }

                // ================= TAB 1: UPDATES (STATUS & CHANNELS) =================
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Text(
                                text = "Status",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                            )
                        }

                        // Horizontal Status Stories Tray
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // My Status Card
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 86.dp, height = 140.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(SurfaceDark)
                                            .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                                            .clickable { onAddStatus() }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF262626)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "Me", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .align(Alignment.BottomEnd)
                                                        .clip(CircleShape)
                                                        .background(AccentWhite),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = OledBlack, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(32.dp))
                                            Text(text = "Add status", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                // Contact 1: Bella
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 86.dp, height = 140.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFF4A261), Color(0xFF5A2D0C))
                                                )
                                            )
                                            .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                                            .clickable { onViewStatus("Bella") }
                                            .padding(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .align(Alignment.TopCenter)
                                                .clip(CircleShape)
                                                .border(2.dp, AccentGreen, CircleShape)
                                                .background(Color(0xFF1E1E1E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "B", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentWhite)
                                        }
                                        Text(
                                            text = "Bella",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentWhite,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        )
                                    }
                                }

                                // Contact 2: Ngozi
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 86.dp, height = 140.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF2A9D8F), Color(0xFF0B3D3A))
                                                )
                                            )
                                            .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                                            .clickable { onViewStatus("Ngozi") }
                                            .padding(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .align(Alignment.TopCenter)
                                                .clip(CircleShape)
                                                .border(2.dp, AccentGreen, CircleShape)
                                                .background(Color(0xFF1E1E1E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "N", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentWhite)
                                        }
                                        Text(
                                            text = "Ngozi",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentWhite,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        )
                                    }
                                }
                            }
                        }

                        // Channels Header
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
                                    fontSize = 18.sp,
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

                        // Channel 1: Lagos Tech
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {}
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E3A8A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "LT", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AccentWhite)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "Lagos Tech", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = AccentBlue, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Three new high-signal hardware startups this week", fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(text = "Yesterday", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                // ================= TAB 2: COMMUNITIES =================
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                                    .clickable {}
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF333333)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "DT", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AccentWhite)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Mestxa Architecture Team", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "4 active channels • MLS TreeKEM secure", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                                    .clickable {}
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF222222)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "FC", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AccentWhite)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Founders Circle", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Private community for audited founders", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // ================= TAB 3: CALLS =================
                3 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Quick Action Tray
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                QuickCallButton(icon = Icons.Default.Phone, label = "Call", onClick = { onStartCall("Amaka") })
                                QuickCallButton(icon = Icons.Default.CalendarToday, label = "Schedule", onClick = {})
                                QuickCallButton(icon = Icons.Default.Dialpad, label = "Keypad", onClick = {})
                            }
                        }

                        item {
                            Text(
                                text = "Recent",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        val filteredCalls = sampleCalls.filter { call ->
                            searchQuery.isBlank() || call.name.contains(searchQuery, ignoreCase = true)
                        }

                        items(filteredCalls) { call ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStartCall(call.name) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(SurfaceDark, CircleShape)
                                        .border(1.dp, BorderHairline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = call.name.take(1),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentWhite
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = call.name,
                                        style = MaterialTheme.typography.labelLarge,
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
                                IconButton(onClick = { onStartCall(call.name) }) {
                                    Icon(
                                        imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = TextPrimary
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

@Composable
fun ChatRow(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(SurfaceDark, CircleShape)
                .border(
                    width = if (chat.unreadCount > 0) 1.5.dp else 1.dp,
                    color = if (chat.unreadCount > 0) AccentWhite else BorderHairline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.take(1),
                fontSize = 17.sp,
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
                    text = chat.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp
                )
                Text(
                    text = chat.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.unreadCount > 0) AccentWhite else TextSecondary,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.unreadCount == 0) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = if (chat.isRead) AccentBlue else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
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
