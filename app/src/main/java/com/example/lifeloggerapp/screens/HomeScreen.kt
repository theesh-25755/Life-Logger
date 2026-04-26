package com.example.lifeloggerapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeloggerapp.data.local.EntryEntity
import com.example.lifeloggerapp.entry.EntryViewModel
import com.example.lifeloggerapp.ui.theme.CreamBackground
import com.example.lifeloggerapp.ui.theme.SageGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.lifecycle.repeatOnLifecycle
import com.example.lifeloggerapp.syncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    entryViewModel: EntryViewModel = viewModel()
) {
    val entries by entryViewModel.entries.collectAsState()

    val todayPrefix = LocalDate.now().toString() // "2025-04-23"
    val todayEntries = entries.filter { it.createdAt?.startsWith(todayPrefix) == true }
    val earlierEntries = entries.filter { it.createdAt?.startsWith(todayPrefix) == false }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        withContext(Dispatchers.IO) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                syncManager.sync()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (entries.any { !it.synced }) Icons.Default.CloudOff
                            else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (entries.any { !it.synced }) Color.Gray else SageGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("My Logs", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = CreamBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = SageGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Log")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                    .uppercase(),
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Daily Overview",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OverviewCard(
                title = "${todayEntries.size} ${if (todayEntries.size == 1) "Log" else "Logs"}",
                subtitle = "Documented today",
                icon = "📝",
                backgroundColor = Color(0xFFEDF2E6)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OverviewCard(
                title = todayEntries.firstOrNull()?.mood?.replaceFirstChar { it.uppercase() } ?: "No logs",
                subtitle = "Latest mood",
                icon = "✨",
                backgroundColor = Color(0xFFF7F7F0)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs yet. Tap + to start!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (todayEntries.isNotEmpty()) {
                        item {
                            SectionLabel("Today")
                        }
                        items(todayEntries) { entry ->
                            EntryTimelineItem(
                                entry = entry,
                                showLine = entry != todayEntries.last()
                            )
                        }
                    }

                    if (earlierEntries.isNotEmpty()) {
                        item {
                            SectionLabel("Earlier")
                        }
                        items(earlierEntries) { entry ->
                            EntryTimelineItem(
                                entry = entry,
                                showLine = entry != earlierEntries.last()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EntryTimelineItem(entry: EntryEntity, showLine: Boolean = true) {
    val moodEmoji = when (entry.mood) {
        "sad"      -> "😢"
        "neutral"  -> "😐"
        "calm"     -> "😊"
        "happy"    -> "😁"
        "ecstatic" -> "🤩"
        else       -> "📝"
    }

    val timeFormatted = entry.createdAt?.let {
        try {
            val instant = Instant.parse(it)
            val local = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("h:mm a").format(local)
        } catch (e: Exception) { "" }
    } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFFC5D1B3),
                border = BorderStroke(2.dp, Color.White)
            ) {}
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(bottom = 24.dp, end = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(moodEmoji, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = entry.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(text = timeFormatted, color = Color.Gray, fontSize = 12.sp)
                }
                if (!entry.body.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = entry.body,
                        color = Color.DarkGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 2
                    )
                }
                if (!entry.synced) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pending sync",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewCard(title: String, subtitle: String, icon: String, backgroundColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SageGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}