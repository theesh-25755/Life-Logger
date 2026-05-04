package com.example.lifeloggerapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeloggerapp.data.local.EntryEntity
import com.example.lifeloggerapp.entry.EntryViewModel
import com.example.lifeloggerapp.syncManager
import com.example.lifeloggerapp.ui.theme.SageGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    entryViewModel: EntryViewModel = viewModel()
) {
    val allEntries by entryViewModel.entries.collectAsState()

    // ── Search & filter state ─────────────────────────────────
    var query by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDateRange by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val queryFlow = remember { MutableStateFlow("") }
    val debouncedValue = remember { mutableStateOf("") }

    LaunchedEffect(query) { queryFlow.value = query }
    LaunchedEffect(Unit) {
        queryFlow.debounce(300).distinctUntilChanged().collect {
            debouncedValue.value = it
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        withContext(Dispatchers.IO) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                syncManager.sync()
            }
        }
    }

    val moods = listOf("😢" to "sad", "😐" to "neutral", "😊" to "calm", "😁" to "happy", "🤩" to "ecstatic")
    val categories = listOf("Workout", "Study", "Personal", "Event")
    val dateRanges = listOf("Today", "This week", "This month", "This year")

    val isSearching = query.isNotBlank() || selectedMood != null || selectedCategory != null || selectedDateRange != null
    val activeFiltersCount = listOfNotNull(selectedMood, selectedCategory, selectedDateRange).size

    // ── Filter logic ──────────────────────────────────────────
    val displayEntries = remember(debouncedValue.value, selectedMood, selectedCategory, selectedDateRange, allEntries) {
        if (!isSearching) return@remember allEntries
        allEntries.filter { entry ->
            val matchesQuery = debouncedValue.value.isBlank() ||
                    entry.title.contains(debouncedValue.value, ignoreCase = true) ||
                    entry.body?.contains(debouncedValue.value, ignoreCase = true) == true
            val matchesMood = selectedMood == null || entry.mood == selectedMood
            val matchesCategory = selectedCategory == null ||
                    entry.category?.equals(selectedCategory, ignoreCase = true) == true
            val matchesDate = selectedDateRange == null || run {
                val entryDate = entry.createdAt?.let {
                    try { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    catch (e: Exception) { null }
                } ?: return@run false
                val today = LocalDate.now()
                when (selectedDateRange) {
                    "Today"      -> entryDate == today
                    "This week"  -> entryDate >= today.minusDays(7)
                    "This month" -> entryDate.month == today.month && entryDate.year == today.year
                    "This year"  -> entryDate.year == today.year
                    else         -> true
                }
            }
            matchesQuery && matchesMood && matchesCategory && matchesDate
        }
    }

    // For non-search mode split by today/earlier
    val todayPrefix = LocalDate.now().toString()
    val todayEntries = displayEntries.filter { it.createdAt?.startsWith(todayPrefix) == true }
    val earlierEntries = displayEntries.filter { it.createdAt?.startsWith(todayPrefix) == false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (allEntries.any { !it.synced }) Icons.Default.CloudOff
                            else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (allEntries.any { !it.synced })
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else SageGreen
                        )
                        Spacer(Modifier.width(8.dp))
//                        Text("My Logs", fontWeight = FontWeight.Bold)
                        Column() {
                            Text(
                                text = LocalDate.now()
                                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                                    .uppercase(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Daily Overview",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = SageGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Log")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // ── Header ────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
//                Spacer(modifier = Modifier.height(16.dp))

//                Text(
//                    text = LocalDate.now()
//                        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
//                        .uppercase(),
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    fontSize = 12.sp,
//                    fontWeight = FontWeight.SemiBold
//                )
//
//                Text(
//                    text = "Daily Overview",
//                    fontSize = 24.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.onBackground,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )

//                Spacer(modifier = Modifier.height(16.dp))
//
//                OverviewCard(
//                    title = "${todayEntries.size} ${if (todayEntries.size == 1) "Log" else "Logs"}",
//                    subtitle = "Documented today",
//                    icon = "📝"
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                OverviewCard(
//                    title = todayEntries.firstOrNull()?.mood?.replaceFirstChar { it.uppercase() }
//                        ?: "No logs",
//                    subtitle = "Latest mood",
//                    icon = "✨"
//                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Search bar ────────────────────────────────
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search entries...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            BadgedBox(
                                badge = {
                                    if (activeFiltersCount > 0) {
                                        Badge(containerColor = SageGreen) {
                                            Text("$activeFiltersCount", fontSize = 9.sp,
                                                color = androidx.compose.ui.graphics.Color.White)
                                        }
                                    }
                                }
                            ) {
                                IconButton(onClick = { showFilters = !showFilters }) {
                                    Icon(
                                        if (showFilters) Icons.Default.FilterList
                                        else Icons.Default.FilterListOff,
                                        contentDescription = "Filter",
                                        tint = if (activeFiltersCount > 0) SageGreen
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Filter panel ──────────────────────────────────
            AnimatedVisibility(visible = showFilters) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Mood", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moods) { (emoji, value) ->
                            FilterChip(
                                selected = selectedMood == value,
                                onClick = { selectedMood = if (selectedMood == value) null else value },
                                label = { Text("$emoji ${value.replaceFirstChar { it.uppercase() }}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = SageGreen
                                )
                            )
                        }
                    }

                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = if (selectedCategory == category) null else category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = SageGreen
                                )
                            )
                        }
                    }

                    Text("Date range", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dateRanges) { range ->
                            FilterChip(
                                selected = selectedDateRange == range,
                                onClick = { selectedDateRange = if (selectedDateRange == range) null else range },
                                label = { Text(range) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = SageGreen
                                )
                            )
                        }
                    }

                    if (activeFiltersCount > 0) {
                        TextButton(
                            onClick = { selectedMood = null; selectedCategory = null; selectedDateRange = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear all filters", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }

            // ── Results count (search mode only) ──────────────
            if (isSearching) {
                Text(
                    text = "${displayEntries.size} result${if (displayEntries.size == 1) "" else "s"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ── Feed ──────────────────────────────────────────
            if (allEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No logs yet. Tap + to start!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium)
                    }
                }
            } else if (isSearching && displayEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No entries found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium)
                    }
                }
            } else if (isSearching) {
                // Flat search results list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayEntries, key = { it.id }) { entry ->
                        SearchResultCard(entry = entry, query = debouncedValue.value, onClick = { onEntryClick(entry.id) })
                    }
                }
            } else {
                // Normal timeline feed
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                ) {
                    if (todayEntries.isNotEmpty()) {
                        item { SectionLabel("Today") }
                        items(todayEntries) { entry ->
                            EntryTimelineItem(
                                entry = entry,
                                showLine = entry != todayEntries.last(),
                                onEntryClick = onEntryClick
                            )
                        }
                    }
                    if (earlierEntries.isNotEmpty()) {
                        item { SectionLabel("Earlier") }
                        items(earlierEntries) { entry ->
                            EntryTimelineItem(
                                entry = entry,
                                showLine = entry != earlierEntries.last(),
                                onEntryClick = onEntryClick
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EntryTimelineItem(entry: EntryEntity, showLine: Boolean = true, onEntryClick: (String) -> Unit = {}) {
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
                color = SageGreen.copy(alpha = 0.5f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
            ) {}
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        }

        Card(
            onClick = { onEntryClick(entry.id) },
            modifier = Modifier
                .padding(bottom = 24.dp, end = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(moodEmoji, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = entry.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = timeFormatted,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                if (!entry.body.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = entry.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!entry.synced) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Pending sync", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(entry: EntryEntity, query: String, onClick: () -> Unit) {
    val moodEmoji = when (entry.mood) {
        "sad"      -> "😢"
        "neutral"  -> "😐"
        "calm"     -> "😊"
        "happy"    -> "😁"
        "ecstatic" -> "🤩"
        else       -> "📝"
    }

    val dateFormatted = entry.createdAt?.let {
        try {
            val instant = Instant.parse(it)
            val local = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            DateTimeFormatter.ofPattern("MMM d, yyyy").format(local)
        } catch (e: Exception) { "" }
    } ?: ""

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(moodEmoji, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = entry.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(text = dateFormatted, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!entry.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            if (!entry.category.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = entry.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewCard(title: String, subtitle: String, icon: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(SageGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}