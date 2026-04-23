package com.example.lifeloggerapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
import com.example.lifeloggerapp.ui.theme.CreamBackground
import com.example.lifeloggerapp.ui.theme.SageGreen

// --- STEP 1: DATA STRUCTURE & GLOBAL LIST ---
// This list lives as long as the app is open.
data class LifeLog(
    val title: String,
    val note: String,
    val mood: String,
    val tag: String,
    val imageUri: Uri? = null,
    val date: String = "TODAY, APRIL 23",
    val time: String = "10:40 PM"
)

// Global list that UI observes for changes
val GlobalLogs = mutableStateListOf<LifeLog>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(onBackClick: () -> Unit) {
    // --- STATE VARIABLES ---
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Personal") }
    var selectedMood by remember { mutableStateOf("😊") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- DATA LISTS ---
    val tags = listOf("Workout", "Study", "Personal", "Event")
    val moods = listOf("😢", "😐", "😊", "😁", "🤩")

    // --- IMAGE PICKER LOGIC ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

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
                    // --- STEP 2: MODIFIED SAVE BUTTON ---
                    TextButton(onClick = {
                        if (title.isNotBlank() && note.isNotBlank()) {
                            // Add the new entry to the start of our global list
                            GlobalLogs.add(0, LifeLog(
                                title = title,
                                note = note,
                                mood = selectedMood,
                                tag = selectedTag,
                                imageUri = selectedImageUri
                            ))
                            onBackClick() // Navigate back to Home
                        }
                    }) {
                        Text("Save", color = SageGreen, fontWeight = FontWeight.Bold)
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
            // Header: Date & Time
            Text("TODAY, APRIL 23 • 10:40 PM", color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // --- MOOD PICKER ---
            Text("How are you feeling?", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moods.forEach { mood ->
                    MoodEmoji(
                        emoji = mood,
                        isSelected = selectedMood == mood,
                        onClick = { selectedMood = mood }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title Input
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Interactive Tags Row
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

            // Image Display Area
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = Color.White
                        )
                    }
                }
            }

            // Action Button: Add Image
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Story Input
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