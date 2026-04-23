package com.example.lifeloggerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeloggerapp.auth.AuthState
import com.example.lifeloggerapp.auth.AuthViewModel
import com.example.lifeloggerapp.ui.theme.SageGreen

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var isDarkMode by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val currentUser = authViewModel.getCurrentUser()
    val userEmail = currentUser?.email ?: "No email"
    // Use the part before @ as display name fallback until profiles table is set up
    val displayName = userEmail.substringBefore("@")

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            authViewModel.resetState()
            onLogout()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Profile Image & Info
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
            IconButton(
                onClick = { /* Edit action — Phase 3 */ },
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(SageGreen)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(userEmail, color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsItem(Icons.Default.Sync, "Sync Status", "All data synced")
                SettingsItem(Icons.Default.CloudQueue, "Backup & Restore", null)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color.Gray)
                        Spacer(Modifier.width(16.dp))
                        Text("Dark Mode")
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                }

                SettingsItem(Icons.Default.Notifications, "Notifications", null)
                SettingsItem(Icons.Default.VerifiedUser, "Privacy Policy", null)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.signOut() },
            enabled = authState !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    color = Color.Red,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard("Total Logs", "—", SageGreen, Modifier.weight(1f))
            StatCard("Streak", "—", Color(0xFFE8EAF6), Modifier.weight(1f))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                color = if (color == SageGreen) Color.White else Color.Gray,
                fontSize = 14.sp
            )
            Text(
                value,
                color = if (color == SageGreen) Color.White else Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}