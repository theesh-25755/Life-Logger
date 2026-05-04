package com.example.lifeloggerapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lifeloggerapp.auth.AuthRepository
import com.example.lifeloggerapp.auth.AuthState
import com.example.lifeloggerapp.auth.AuthViewModel
import com.example.lifeloggerapp.entry.EntryViewModel
import com.example.lifeloggerapp.syncManager
import com.example.lifeloggerapp.SyncState
import com.example.lifeloggerapp.ui.theme.SageGreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.lifeloggerapp.auth.ProfileData

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    entryViewModel: EntryViewModel = viewModel(),
    isDarkMode: Boolean = false,
    onDarkModeToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    val authState by authViewModel.authState.collectAsState()
    val entries by entryViewModel.entries.collectAsState()
    val syncState by syncManager.syncState.collectAsState()

    val currentUser = authViewModel.getCurrentUser()
    val userEmail = currentUser?.email ?: "No email"

    // Profile state
    var displayName by remember { mutableStateOf(userEmail.substringBefore("@")) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    // Dialog state
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(displayName) }

    // Stats
    val totalLogs = entries.size
    val todayCount = entries.count {
        it.createdAt?.startsWith(LocalDate.now().toString()) == true
    }

    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                profileImageUri = it
                val userId = currentUser?.id ?: return@let
                scope.launch {
                    val result = authRepository.uploadProfileImage(userId, it, context)
                    if (result.isSuccess) {
                        profileImageUrl = result.getOrNull()
                    }
                }
            }
        }
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            authViewModel.resetState()
            onLogout()
        }
    }

    // Load profile from Supabase on open
    LaunchedEffect(currentUser?.id) {
        val userId = currentUser?.id ?: return@LaunchedEffect
        val result = authRepository.getProfile(userId)
        if (result.isSuccess) {
            val profile: ProfileData? = result.getOrNull()
            val name = profile?.displayName
            val url = profile?.avatarUrl
            if (!name.isNullOrBlank()) displayName = name
            if (!url.isNullOrBlank()) profileImageUrl = url
        }
    }

    // ── Edit name dialog ──────────────────────────────────────
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit display name") },
            text = {
                OutlinedTextField(
                    value = editNameValue,
                    onValueChange = { editNameValue = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editNameValue.isNotBlank()) {
                            displayName = editNameValue.trim()
                            val userId = currentUser?.id ?: return@TextButton
                            scope.launch {
                                authRepository.updateDisplayName(userId, editNameValue.trim())
                            }
                        }
                        showEditNameDialog = false
                    }
                ) { Text("Save", color = SageGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Privacy policy dialog ─────────────────────────────────
    if (showPrivacyDialog) {
        Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Effective date: May 2025", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp))

                    PrivacySection("Data We Collect",
                        "LifeLog collects your email address for authentication purposes. Journal entries, images, and audio notes you create are stored securely in your personal account and are not accessible to other users.")

                    PrivacySection("How We Use Your Data",
                        "Your data is used solely to provide the LifeLog journaling experience. We do not sell, share, or use your personal data for advertising or analytics beyond basic app functionality.")

                    PrivacySection("Data Storage",
                        "All data is stored using Supabase, a secure cloud platform with end-to-end encryption in transit. A local copy is maintained on your device for offline access.")

                    PrivacySection("Data Retention",
                        "Your data is retained as long as your account is active. You may delete individual entries at any time. To request full account deletion, contact us directly.")

                    PrivacySection("Your Rights",
                        "You have the right to access, correct, or delete your personal data at any time. You may also export your data on request.")

                    PrivacySection("Contact",
                        "For any privacy concerns or data requests, please contact us at support@lifelog.app.")

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showPrivacyDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Close") }
                }
            }
        }
    }

    // ── Main UI ───────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Avatar with tap to change
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, SageGreen.copy(alpha = 0.4f), CircleShape)
                    .clickable {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (!profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SageGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display name with edit tap
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showEditNameDialog = true }
        ) {
            Text(
                displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit name",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            userEmail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stat cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Total Logs", totalLogs.toString(), Modifier.weight(1f), primary = true)
            StatCard("Today", todayCount.toString(), Modifier.weight(1f), primary = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Sync status — live
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (syncState) {
                                is SyncState.Syncing -> Icons.Default.Sync
                                is SyncState.Error   -> Icons.Default.SyncProblem
                                else                 -> Icons.Default.CloudDone
                            },
                            contentDescription = null,
                            tint = when (syncState) {
                                is SyncState.Syncing -> MaterialTheme.colorScheme.onSurfaceVariant
                                is SyncState.Error   -> MaterialTheme.colorScheme.error
                                else                 -> SageGreen
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Sync Status", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                when (syncState) {
                                    is SyncState.Syncing -> "Syncing..."
                                    is SyncState.Error   -> "Sync error"
                                    else                 -> "All data synced"
                                },
                                fontSize = 12.sp,
                                color = when (syncState) {
                                    is SyncState.Error -> MaterialTheme.colorScheme.error
                                    else               -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    if (syncState is SyncState.Error) {
                        TextButton(onClick = { scope.launch { syncManager.sync() } }) {
                            Text("Retry", color = SageGreen, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Dark mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Nightlight, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onDarkModeToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SageGreen
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Privacy policy
                SettingsItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "Privacy Policy",
                    subtitle = null,
                    onClick = { showPrivacyDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout
        Button(
            onClick = { authViewModel.signOut() },
            enabled = authState !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PrivacySection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(body, fontSize = 12.sp, lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier, primary: Boolean) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (primary) SageGreen else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                color = if (primary) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}