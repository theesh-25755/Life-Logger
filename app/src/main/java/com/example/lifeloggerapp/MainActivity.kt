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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lifeloggerapp.auth.AuthViewModel
import com.example.lifeloggerapp.screens.HomeScreen
import com.example.lifeloggerapp.ui.screens.*
import com.example.lifeloggerapp.ui.theme.LifeLoggerAppTheme
import com.russhwolf.settings.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeLoggerAppTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // Check if user is already logged in
//    val startDestination = if (BuildConfig.DEBUG || authViewModel.isLoggedIn()) "home" else "login"
    val startDestination = if (authViewModel.isLoggedIn()) "home" else "login"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf("home", "calendar", "insights", "profile")

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                BottomNavigationBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") },
                    authViewModel = authViewModel
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() },
                    authViewModel = authViewModel
                )
            }
            composable("home") {
                HomeScreen(onAddClick = { navController.navigate("new_entry") })
            }
//            composable("calendar") { CalendarScreen() }
//            composable("insights") { InsightsScreen() }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable("new_entry") {
                NewEntryScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: androidx.navigation.NavController,
    currentRoute: String?
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { navigateTo(navController, "home", currentRoute) }
        )
//        NavigationBarItem(
//            icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
//            label = { Text("Calendar") },
//            selected = currentRoute == "calendar",
//            onClick = { navigateTo(navController, "calendar", currentRoute) }
//        )
//        NavigationBarItem(
//            icon = { Icon(Icons.Default.Assessment, contentDescription = "Insights") },
//            label = { Text("Insights") },
//            selected = currentRoute == "insights",
//            onClick = { navigateTo(navController, "insights", currentRoute) }
//        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { navigateTo(navController, "profile", currentRoute) }
        )
    }
}

private fun navigateTo(
    navController: androidx.navigation.NavController,
    route: String,
    currentRoute: String?
) {
    if (currentRoute != route) {
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}