package com.example.lifeloggerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lifeloggerapp.ui.screens.*
import com.example.lifeloggerapp.ui.theme.LifeLoggerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeLoggerAppTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        // Only show bottom bar on main navigation screens
                        if (currentRoute != "new_entry") {
                            BottomNavigationBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(onAddClick = { navController.navigate("new_entry") })
                        }
                        composable("calendar") { CalendarScreen() }
                        composable("insights") { InsightsScreen() }
                        composable("profile") { ProfileScreen() }
                        composable("new_entry") {
                            NewEntryScreen(onBackClick = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavController, currentRoute: String?) {
    NavigationBar {
        // --- HOME ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { navigateTo(navController, "home", currentRoute) }
        )

        // --- CALENDAR (Added) ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
            label = { Text("Calendar") },
            selected = currentRoute == "calendar",
            onClick = { navigateTo(navController, "calendar", currentRoute) }
        )

        // --- INSIGHTS (Added) ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Assessment, contentDescription = "Insights") },
            label = { Text("Insights") },
            selected = currentRoute == "insights",
            onClick = { navigateTo(navController, "insights", currentRoute) }
        )

        // --- PROFILE ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { navigateTo(navController, "profile", currentRoute) }
        )
    }
}

// Helper function to handle clean navigation
private fun navigateTo(navController: androidx.navigation.NavController, route: String, currentRoute: String?) {
    if (currentRoute != route) {
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}