package com.example.lifeloggerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeloggerapp.ui.theme.CreamBackground
import com.example.lifeloggerapp.ui.theme.SageGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(onBackClick: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                    TextButton(onClick = { /* Save logic later */ }) {
                        Text("Save", color = SageGreen, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = CreamBackground
    ) { innerPadding -> // This is the padding from the Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Fix 1: Removed 'paddingValues ='
                .padding(16.dp)        // Fix 2: Removed 'all ='
        ) {
            Text("Today, October 24, 2023 • 09:42 AM", color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Title Input
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                // Fix 3: Using simpler color settings
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                TagItem("Workout", isSelected = true)
                Spacer(modifier = Modifier.width(8.dp))
                TagItem("Study", isSelected = false)
                Spacer(modifier = Modifier.width(8.dp))
                TagItem("Personal", isSelected = false)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Story Input
            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Tell your story...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
fun TagItem(name: String, isSelected: Boolean) {
    Surface(
        color = if (isSelected) Color(0xFFE1E8D1) else Color(0xFFEEEEEE),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}