package com.example.lifeloggerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lifeloggerapp.entry.EntryState
import com.example.lifeloggerapp.entry.EntryViewModel
import com.example.lifeloggerapp.ui.theme.CreamBackground
import com.example.lifeloggerapp.ui.theme.SageGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    onBackClick: () -> Unit,
    entryViewModel: EntryViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Personal") }
    var selectedMood by remember { mutableStateOf("neutral") }

    val entryState by entryViewModel.entryState.collectAsState()

    val tags = listOf("Workout", "Study", "Personal", "Event")

    // Mood options: emoji for display, value for storage
    val moods = listOf(
        "😢" to "sad",
        "😐" to "neutral",
        "😊" to "calm",
        "😁" to "happy",
        "🤩" to "ecstatic"
    )

    LaunchedEffect(entryState) {
        if (entryState is EntryState.Success) {
            entryViewModel.resetState()
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                entryViewModel.createEntry(
                                    title = title,
                                    body = note.ifBlank { null },
                                    mood = selectedMood,
                                    category = selectedTag,
                                    tags = listOf(selectedTag)
                                )
                            }
                        },
                        enabled = entryState !is EntryState.Loading && title.isNotBlank()
                    ) {
                        if (entryState is EntryState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SageGreen
                            )
                        } else {
                            Text("Save", color = SageGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        containerColor = CreamBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                    .uppercase(),
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("How are you feeling?", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moods.forEach { (emoji, value) ->
                    MoodEmoji(
                        emoji = emoji,
                        isSelected = selectedMood == value,
                        onClick = { selectedMood = value }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    TagItem(
                        name = tag,
                        isSelected = selectedTag == tag,
                        onTagClick = { selectedTag = tag }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entryState is EntryState.Error) {
                Text(
                    text = (entryState as EntryState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Tell your story...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun MoodEmoji(emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFE1E8D1) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 24.sp)
    }
}

@Composable
fun TagItem(name: String, isSelected: Boolean, onTagClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color(0xFFE1E8D1) else Color(0xFFEEEEEE),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onTagClick() }
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (isSelected) SageGreen else Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}