package com.example.lifeloggerapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lifeloggerapp.auth.AuthRepository
import com.example.lifeloggerapp.entry.EntryState
import com.example.lifeloggerapp.entry.EntryViewModel
import com.example.lifeloggerapp.entry.MediaState
import com.example.lifeloggerapp.entry.MediaViewModel
import com.example.lifeloggerapp.entry.MediaViewModelFactory
import com.example.lifeloggerapp.ui.theme.SageGreen
import java.io.File

data class AudioEntry(
    val filePath: String,
    val durationSec: Int,
    val amplitudeSamples: List<Float>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    onBackClick: () -> Unit,
    entryViewModel: EntryViewModel = viewModel()
) {
    val context = LocalContext.current
    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(context))
    val authRepository = remember { AuthRepository() }

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Personal") }
    var selectedMood by remember { mutableStateOf("neutral") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var createdEntryId by remember { mutableStateOf<String?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var audioFilePath by remember { mutableStateOf<String?>(null) }
    var recordingDuration by remember { mutableStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var amplitudeSamples by remember { mutableStateOf<List<Float>>(emptyList()) }
    var audioEntries by remember { mutableStateOf<List<AudioEntry>>(emptyList()) }

    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var playingIndex by remember { mutableStateOf<Int?>(null) }

    val entryState by entryViewModel.entryState.collectAsState()
    val mediaState by mediaViewModel.mediaState.collectAsState()

    val tags = listOf("Workout", "Study", "Personal", "Event")
    val moods = listOf(
        "😢" to "sad", "😐" to "neutral", "😊" to "calm", "😁" to "happy", "🤩" to "ecstatic"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
        onResult = { uris -> selectedImageUris = (selectedImageUris + uris).distinct().take(3) }
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                audioFilePath = file.absolutePath
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else { @Suppress("DEPRECATION") MediaRecorder() }
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare(); start()
                }
                mediaRecorder = recorder
                isRecording = true
                recordingDuration = 0
                amplitudeSamples = emptyList()
            }
        }
    )

    LaunchedEffect(entryState) {
        if (entryState is EntryState.Success) {
            val entryId = createdEntryId
            val userId = authRepository.getCurrentUserId()
            if (entryId != null && userId != null) {
                selectedImageUris.forEach { uri -> mediaViewModel.uploadImage(entryId, userId, uri) }
                audioEntries.forEach { audio -> mediaViewModel.uploadAudio(entryId, userId, audio.filePath, audio.durationSec) }
            }
            if (selectedImageUris.isEmpty() && audioEntries.isEmpty()) { entryViewModel.resetState(); onBackClick() }
        }
    }

    LaunchedEffect(mediaState) {
        if (mediaState is MediaState.Success || mediaState is MediaState.Error) {
            mediaViewModel.resetState(); entryViewModel.resetState(); onBackClick()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val samples = mutableListOf<Float>()
            var seconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(100)
                samples.add(mediaRecorder?.maxAmplitude?.toFloat() ?: 0f)
                amplitudeSamples = samples.toList()
                if (samples.size % 10 == 0) { seconds++; recordingDuration = seconds }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                kotlinx.coroutines.delay(100)
                val p = mediaPlayer
                if (p != null && p.isPlaying) playbackProgress = p.currentPosition.toFloat() / p.duration.toFloat()
            }
        } else { playbackProgress = 0f }
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer?.release(); mediaPlayer = null } }

    fun startRecording() {
        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        audioFilePath = file.absolutePath
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else { @Suppress("DEPRECATION") MediaRecorder() }
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare(); start()
        }
        mediaRecorder = recorder; isRecording = true; recordingDuration = 0; amplitudeSamples = emptyList()
    }

    fun stopRecording() {
        mediaRecorder?.stop(); mediaRecorder?.release(); mediaRecorder = null; isRecording = false
        audioFilePath?.let { path ->
            audioEntries = audioEntries + AudioEntry(path, recordingDuration, amplitudeSamples)
        }
        audioFilePath = null; amplitudeSamples = emptyList(); recordingDuration = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isSaving = entryState is EntryState.Loading || mediaState is MediaState.Uploading
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (isRecording) stopRecording()
                                entryViewModel.createEntry(
                                    title = title,
                                    body = note.ifBlank { null },
                                    mood = selectedMood,
                                    category = selectedTag,
                                    tags = listOf(selectedTag),
                                    onCreated = { id -> createdEntryId = id }
                                )
                            }
                        },
                        enabled = !isSaving && title.isNotBlank()
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SageGreen)
                        else Text("Save", color = SageGreen, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text(
                text = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                    .uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("How are you feeling?", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moods.forEach { (emoji, value) ->
                    MoodEmoji(emoji = emoji, isSelected = selectedMood == value, onClick = { selectedMood = value })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = title,
                onValueChange = { input -> title = if (input.isNotEmpty()) input.replaceFirstChar { it.uppercase() } else input },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                placeholder = { Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag -> TagItem(name = tag, isSelected = selectedTag == tag, onTagClick = { selectedTag = tag }) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entryState is EntryState.Error) {
                Text(text = (entryState as EntryState.Error).message, color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Tell your story...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image previews
            if (selectedImageUris.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedImageUris.forEach { uri ->
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            AsyncImage(model = uri, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris - uri },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    repeat(3 - selectedImageUris.size) { Box(modifier = Modifier.weight(1f)) }
                }
                if (selectedImageUris.size < 3) {
                    Text(
                        text = "${3 - selectedImageUris.size} more photo${if (3 - selectedImageUris.size == 1) "" else "s"} can be added",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Audio waveform players
            audioEntries.forEachIndexed { index, audio ->
                AudioWaveformPlayer(
                    durationSec = audio.durationSec,
                    amplitudeSamples = audio.amplitudeSamples,
                    isPlaying = isPlaying && playingIndex == index,
                    playbackProgress = if (playingIndex == index) playbackProgress else 0f,
                    onPlayPause = {
                        if (isPlaying && playingIndex == index) { mediaPlayer?.pause(); isPlaying = false }
                        else {
                            if (playingIndex != index) { mediaPlayer?.release(); mediaPlayer = null; isPlaying = false; playbackProgress = 0f }
                            if (mediaPlayer == null) {
                                try {
                                    mediaPlayer = android.media.MediaPlayer().apply {
                                        setDataSource(audio.filePath); prepare(); start()
                                        setOnCompletionListener { isPlaying = false; playbackProgress = 0f; playingIndex = null; release(); mediaPlayer = null }
                                    }
                                } catch (e: Exception) { return@AudioWaveformPlayer }
                            } else { mediaPlayer?.start() }
                            playingIndex = index; isPlaying = true
                        }
                    },
                    onRemove = {
                        if (playingIndex == index) { mediaPlayer?.release(); mediaPlayer = null; isPlaying = false; playbackProgress = 0f; playingIndex = null }
                        audioEntries = audioEntries - audio
                    }
                )
            }

            if (isRecording) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Recording... ${recordingDuration}s", fontSize = 13.sp, color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedImageUris.size < 3) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(6.dp), border = BorderStroke(1.5.dp, SageGreen)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = SageGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Image", fontWeight = FontWeight.SemiBold, color = SageGreen)
                    }
                }
                if (audioEntries.size < 3) {
                    OutlinedButton(
                        onClick = {
                            if (isRecording) stopRecording()
                            else {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) startRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = if (isRecording) BorderStroke(1.5.dp, Color.Red) else BorderStroke(1.5.dp, SageGreen),
                        colors = if (isRecording) ButtonDefaults.outlinedButtonColors(containerColor = Color.Red) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null,
                            tint = if (isRecording) Color.White else SageGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRecording) "Stop" else "Record", color = if (isRecording) Color.White else SageGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun AudioWaveformPlayer(
    durationSec: Int,
    amplitudeSamples: List<Float>,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlayPause: () -> Unit,
    onRemove: () -> Unit
) {
    val maxAmp = amplitudeSamples.maxOrNull()?.takeIf { it > 0 } ?: 1f
    val displayBars = 40
    val bars = if (amplitudeSamples.isEmpty()) List(displayBars) { 0.15f }
    else List(displayBars) { i ->
        val index = (i.toFloat() / displayBars * amplitudeSamples.size).toInt().coerceIn(0, amplitudeSamples.size - 1)
        (amplitudeSamples[index] / maxAmp).coerceIn(0.05f, 1f)
    }
    val playedBars = (playbackProgress * displayBars).toInt()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp).background(SageGreen, CircleShape)) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                bars.forEachIndexed { index, amplitude ->
                    Box(modifier = Modifier.weight(1f).height((amplitude * 32).dp.coerceIn(4.dp, 32.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < playedBars) SageGreen else SageGreen.copy(alpha = 0.3f)))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "%d:%02d".format(durationSec / 60, durationSec % 60), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MoodEmoji(emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(50.dp).clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 24.sp) }
}

@Composable
fun TagItem(name: String, isSelected: Boolean, onTagClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onTagClick() }
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (isSelected) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}