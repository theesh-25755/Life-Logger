package com.example.lifeloggerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lifeloggerapp.ui.screens.HomeScreen
import com.example.lifeloggerapp.ui.screens.NewEntryScreen
import com.example.lifeloggerapp.ui.theme.LifeLoggerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeLoggerAppTheme {
                // We call the Navigation function instead of just one screen
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    // This is the "GPS" that remembers which screen you are on
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home" // The app starts on the Home screen
    ) {
        // Route 1: The Home Screen
        composable("home") {
            HomeScreen(onAddClick = {
                // When "+" is clicked, go to the new_entry route
                navController.navigate("new_entry")
            })
        }

        // Route 2: The New Entry Screen
        composable("new_entry") {
            NewEntryScreen(onBackClick = {
                // When back is clicked, go back to the previous screen
                navController.popBackStack()
            })
        }
    }
}