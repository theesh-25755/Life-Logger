package com.example.lifeloggerapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeloggerapp.ui.theme.CreamBackground
import com.example.lifeloggerapp.ui.theme.SageGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onAddClick: () -> Unit) {
    // Calculate how many logs we have dynamically
    val logCount = GlobalLogs.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = SageGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("My Logs", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings Action */ }) {
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
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Log"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            // Header Section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TODAY, APRIL 23",
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

            // --- DYNAMIC OVERVIEW CARDS ---
            OverviewCard(
                title = "$logCount ${if (logCount == 1) "Log" else "Logs"}",
                subtitle = "Documented today",
                icon = "📝",
                backgroundColor = Color(0xFFEDF2E6)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OverviewCard(
                title = if (GlobalLogs.isEmpty()) "No Logs" else GlobalLogs.first().mood,
                subtitle = "Latest mood level",
                icon = "✨",
                backgroundColor = Color(0xFFF7F7F0)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- DYNAMIC TIMELINE ---
            if (GlobalLogs.isEmpty()) {
                // Show a friendly message if there are no logs
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs yet. Tap + to start!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(GlobalLogs) { log ->
                        TimelineItem(
                            time = log.time,
                            title = log.title,
                            description = log.note,
                            mood = log.mood,
                            // Only show the line if it's NOT the last item in the list
                            showLine = log != GlobalLogs.last()
                        )
                    }
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

@Composable
fun TimelineItem(
    time: String,
    title: String,
    description: String,
    mood: String = "",
    showLine: Boolean = true
) {
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
                        if (mood.isNotEmpty()) {
                            Text(mood, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(text = time, color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}