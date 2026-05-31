package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WeatherViewModel
import com.example.ui.WeatherViewModelFactory
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.SatelliteScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize ViewModel with factory
                val weatherViewModel: WeatherViewModel = viewModel(
                    factory = WeatherViewModelFactory(application)
                )

                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0B0E14),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF161B22),
                            contentColor = Color.White,
                            modifier = Modifier.testTag("app_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("Today") },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.Cloud else Icons.Outlined.Cloud,
                                        contentDescription = "Today's Forecast"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color(0xFF22D3EE),
                                    indicatorColor = Color(0x3322D3EE),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("nav_today")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("Satellite") },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Default.Map else Icons.Outlined.Map,
                                        contentDescription = "Satellite & Map tracking"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color(0xFF22D3EE),
                                    indicatorColor = Color(0x3322D3EE),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("nav_satellite")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                label = { Text("Alerts") },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsActive,
                                        contentDescription = "Active Cyclone Warnings"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color(0xFF22D3EE),
                                    indicatorColor = Color(0x3322D3EE),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag("nav_alerts")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> TodayScreen(
                            viewModel = weatherViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> SatelliteScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                        2 -> AlertsScreen(
                            viewModel = weatherViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
