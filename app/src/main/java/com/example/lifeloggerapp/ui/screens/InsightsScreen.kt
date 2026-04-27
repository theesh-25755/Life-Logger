package com.example.lifeloggerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifeloggerapp.ui.theme.SageGreen

@Composable
fun InsightsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Text("Insights", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Reflection on your habits and activities.", color = Color.Gray, fontSize = 14.sp)
        }

        // 1. Top Stats Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallStatCard("Total Entries", "1,284", "+12", Modifier.weight(1f))
                SmallStatCard("This Week", "42", "avg", Modifier.weight(1f))
            }
        }

        // 2. Streak Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SageGreen),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Streak", color = Color.White.copy(alpha = 0.8f))
                        Text("15 Days", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        }

        // 3. Activity Trends (Simple Bar Chart)
        item {
            ChartCard(title = "Activity Trends") {
                Row(
                    modifier = Modifier.height(150.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Bar(0.6f, "Mon")
                    Bar(0.8f, "Tue")
                    Bar(1.0f, "Wed", isSelected = true)
                    Bar(0.4f, "Thu")
                    Bar(0.7f, "Fri")
                    Bar(0.3f, "Sat")
                    Bar(0.2f, "Sun")
                }
            }
        }

        // 4. Milestones Section
        item {
            Text("Key Milestones", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            MilestoneItem("1,000 Entry Achievement", "Unlocked last Tuesday")
        }
    }
}

@Composable
fun SmallStatCard(title: String, value: String, subValue: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(subValue, color = SageGreen, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun Bar(fraction: Float, label: String, isSelected: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight(fraction)
                .background(
                    if (isSelected) SageGreen else Color(0xFFDDE2D5),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun MilestoneItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // This is the line that had the error! Fixed with CircleShape import.
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE8F5E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SageGreen)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}