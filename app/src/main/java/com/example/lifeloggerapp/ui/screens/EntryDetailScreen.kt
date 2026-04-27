package com.example.lifeloggerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lifeloggerapp.data.local.MediaEntity
import com.example.lifeloggerapp.entry.EntryViewModel
import com.example.lifeloggerapp.entry.MediaViewModel
import com.example.lifeloggerapp.entry.MediaViewModelFactory
import com.example.lifeloggerapp.ui.theme.SageGreen
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit = {},
    entryViewModel: EntryViewModel = viewModel()
) {
    val context = LocalContext.current
    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(context))

    val entries by entryViewModel.entries.collectAsState()
    val entry = entries.firstOrNull { it.id == entryId }
    val mediaList by mediaViewModel.entryMedia.collectAsState()

    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playingMediaId by remember { mutableStateOf<String?>(null) }
    var playbackProgress by remember { mutableStateOf(0f) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        mediaViewModel.loadMediaForEntry(entryId)
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                kotlinx.coroutines.delay(100)
                val p = mediaPlayer
                if (p != null && p.isPlaying) {
                    playbackProgress = p.currentPosition.toFloat() / p.duration.toFloat()
                }
            }
        } else {
            playbackProgress = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Format timestamps
    val dateFormatted = entry?.createdAt?.let {
        try {
            val instant = Instant.parse(it)
            val local = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("EEEE, MMMM d · h:mm a").format(local)
        } catch (e: Exception) { it }
    } ?: ""

    val moodEmoji = when (entry?.mood) {
        "sad"      -> "😢 Sad"
        "neutral"  -> "😐 Neutral"
        "calm"     -> "😊 Calm"
        "happy"    -> "😁 Happy"
        "ecstatic" -> "🤩 Ecstatic"
        else       -> ""
    }

    val images = mediaList.filter { it.type == "image" }
    val audios = mediaList.filter { it.type == "audio" }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry") },
            text = { Text("This entry will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryViewModel.deleteEntry(entryId)
                        showDeleteDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(entryId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SageGreen)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Category chip
            if (!entry.category.isNullOrBlank()) {
                Text(
                    text = entry.category.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Title
            Text(
                text = entry.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date + mood row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = dateFormatted,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (moodEmoji.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = moodEmoji, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            // Body text
            if (!entry.body.isNullOrBlank()) {
                Text(
                    text = entry.body,
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Pull quote
            if (!entry.pullQuote.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(SageGreen)
                            .align(Alignment.CenterStart)
                    )
                    Text(
                        text = "\"${entry.pullQuote}\"",
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 30.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Images
            if (images.isNotEmpty()) {
                images.forEach { media ->
                    if (!media.publicUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = media.publicUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .aspectRatio(16f / 9f),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Audio players
            if (audios.isNotEmpty()) {
                audios.forEach { media ->
                    DetailAudioPlayer(
                        media = media,
                        isPlaying = isPlaying && playingMediaId == media.id,
                        playbackProgress = if (playingMediaId == media.id) playbackProgress else 0f,
                        onPlayPause = {
                            if (isPlaying && playingMediaId == media.id) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                if (playingMediaId != media.id) {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    isPlaying = false
                                    playbackProgress = 0f
                                }
                                if (mediaPlayer == null && !media.publicUrl.isNullOrBlank()) {
                                    try {
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(media.publicUrl)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                isPlaying = false
                                                playbackProgress = 0f
                                                playingMediaId = null
                                                release()
                                                mediaPlayer = null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        return@DetailAudioPlayer
                                    }
                                } else {
                                    mediaPlayer?.start()
                                }
                                playingMediaId = media.id
                                isPlaying = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tags
            if (!entry.category.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "# ${entry.category.lowercase()}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!entry.synced) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⏳ Pending sync",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailAudioPlayer(
    media: MediaEntity,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlayPause: () -> Unit
) {
    val displayBars = 40
    val bars = List(displayBars) { i -> (0.1f + 0.9f * Math.sin(i * 0.4).toFloat().coerceIn(0f, 1f)) }
    val playedBars = (playbackProgress * displayBars).toInt()
    val durationSec = media.durationSec ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(40.dp)
                    .background(SageGreen, CircleShape)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bars.forEachIndexed { index, amplitude ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((amplitude * 32).dp.coerceIn(4.dp, 32.dp))
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index < playedBars) SageGreen
                                else SageGreen.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "%d:%02d".format(durationSec / 60, durationSec % 60),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}