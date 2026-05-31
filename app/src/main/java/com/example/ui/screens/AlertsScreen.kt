package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AlertPreference
import com.example.data.database.WeatherAlert
import com.example.ui.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val preferences by viewModel.alertPreferences.collectAsStateWithLifecycle()
    val activeLocationName by viewModel.activeLocationName.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Feed, 1: Preferences

    // Find preference state helpers
    val heavyRainPref = preferences.find { it.alertType == "HEAVY_RAIN" } ?: AlertPreference("HEAVY_RAIN", true, 10.0)
    val strongWindPref = preferences.find { it.alertType == "STRONG_WIND" } ?: AlertPreference("STRONG_WIND", true, 35.0)
    val stormPref = preferences.find { it.alertType == "STORM_SURGE" } ?: AlertPreference("STORM_SURGE", true, 95.0)
    val tempPref = preferences.find { it.alertType == "EXTREME_TEMP" } ?: AlertPreference("EXTREME_TEMP", false, 40.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)) // Immersive deep dark
            .statusBarsPadding()
    ) {
        // Top tab switcher
        Surface(
            color = Color(0xFF161B22), // Immersive dark header
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DISASTER ALERT DESK",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF22D3EE), // Cyan glow
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Emergency Warning Center",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF0B0E14),
                    contentColor = Color(0xFF22D3EE),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = Color(0xFF22D3EE)
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Active Hazards (${alerts.size})") },
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts history feed") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Set Thresholds") },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "Configure trigger rules") }
                    )
                }
            }
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab == 0) {
                ActiveHazardsFeed(
                    alerts = alerts,
                    activeLocation = activeLocationName,
                    onSimulateCyclone = {
                        viewModel.triggerSimulationWarning(
                            locationName = activeLocationName,
                            title = "Cyclone Severe Storm Signal No. 10",
                            message = "Emergency! Extreme wind velocity over 120 km/h tracking towards $activeLocationName coast! Rapid land strike expected in 15-30 minutes. Seek official concrete storm shelter immediately!",
                            intensity = "Extreme"
                        )
                    },
                    onSimulateFlooding = {
                        viewModel.triggerSimulationWarning(
                            locationName = activeLocationName,
                            title = "Torrential Flood Red warning",
                            message = "High danger: Massive precipitation intensity exceeding 45mm/h detected in $activeLocationName district. Local roads flooding in 15-30 minutes. Disconnect lower levels power grids.",
                            intensity = "Severe"
                        )
                    }
                )
            } else {
                AlertRulesSetup(
                    heavyRainPref = heavyRainPref,
                    strongWindPref = strongWindPref,
                    stormPref = stormPref,
                    tempPref = tempPref,
                    onPrefUpdate = { type, enabled, value ->
                        viewModel.updateAlertPreference(type, enabled, value)
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveHazardsFeed(
    alerts: List<WeatherAlert>,
    activeLocation: String,
    onSimulateCyclone: () -> Unit,
    onSimulateFlooding: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simulator Warning Trigger deck
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Simulation station icon", tint = Color(0xFFEF4444))
                    Text(
                        text = "ALERT PUSH SIMULATOR (TEST SYSTEM)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }

                Text(
                    text = "AI Studio lacks severe live active weather right now. Tap a simulation button below to trigger immediate local warnings and push notifications scheduled 15-30 mins prior to the mock event for $activeLocation.",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSimulateCyclone,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Cyclone, contentDescription = "Cyclone storm simulation icon", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simulate Cyclone", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onSimulateFlooding,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Water, contentDescription = "Heavy rain flood simulation icon", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simulate Heavy Rain", fontSize = 11.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B))

        // Warning feeds list representation
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0x3322C55E), RoundedCornerShape(100)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Safety checks clear", tint = Color(0xFF22C55E), modifier = Modifier.size(44.dp))
                    }
                    Text(
                        text = "No Active Extreme Hazards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "There are no storm or torrential warnings active for your coordinates. Skies are normal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(260.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts) { alert ->
                    HazardAlertCard(alert = alert)
                }
            }
        }
    }
}

@Composable
fun HazardAlertCard(alert: WeatherAlert) {
    val badgeColor = when (alert.intensity) {
        "Extreme" -> Color(0xFFEF4444) // Red
        "Severe" -> Color(0xFFF97316)  // Orange
        else -> Color(0xFF0284C7)      // Blue
    }

    val timestampStr = remember(alert.timestamp) {
        val sdf = SimpleDateFormat("h:mm a, d MMM yyyy", Locale.getDefault())
        sdf.format(Date(alert.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = alert.intensity.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = timestampStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )

            HorizontalDivider(color = Color(0xFF1E293B))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Advisory location", tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                Text(
                    text = "Target Area: ${alert.locationName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun AlertRulesSetup(
    heavyRainPref: AlertPreference,
    strongWindPref: AlertPreference,
    stormPref: AlertPreference,
    tempPref: AlertPreference,
    onPrefUpdate: (String, Boolean, Double) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Notification Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Modify storm trigger thresholds to filter custom localized alerts. When weather factors violate the settings below, push alerts activate.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        HorizontalDivider(color = Color(0xFF1E293B))

        // Rule 1: Heavy Rainfall
        RulePreferenceToggle(
            title = "Heavy Rainfall Warning",
            description = "Fires when predicted rainfall density crosses safety parameters prior to severe downpour cell arrivals.",
            isEnabled = heavyRainPref.isEnabled,
            value = heavyRainPref.thresholdValue,
            unit = " mm/hr",
            valueRange = 5.0f..40.0f,
            onUpdate = { enabled, valNew ->
                onPrefUpdate("HEAVY_RAIN", enabled, valNew)
            }
        )

        HorizontalDivider(color = Color(0xFF1E293B))

        // Rule 2: Strong Wind
        RulePreferenceToggle(
            title = "Cyclone / Strong Gale Warning",
            description = "Fires warnings directly when storm system wind gusts challenge safety thresholds.",
            isEnabled = strongWindPref.isEnabled,
            value = strongWindPref.thresholdValue,
            unit = " km/h",
            valueRange = 15.0f..90.0f,
            onUpdate = { enabled, valNew ->
                onPrefUpdate("STRONG_WIND", enabled, valNew)
            }
        )

        HorizontalDivider(color = Color(0xFF1E293B))

        // Rule 3: Storm Surge Code Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lightning & Cyclone Cells", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "Fires alerts when radar tracks active thunderstorms, lighting structures, or tropical depressions over the zone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    lineHeight = 14.sp
                )
            }
            Switch(
                checked = stormPref.isEnabled,
                onCheckedChange = { onPrefUpdate("STORM_SURGE", it, stormPref.thresholdValue) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF22D3EE),
                    checkedTrackColor = Color(0xFF0C4A6E)
                )
            )
        }

        HorizontalDivider(color = Color(0xFF1E293B))

        // Rule 4: Extreme Temp warning
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ambient Temperature Advisories", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "Alerts on cold waves or excessive micro-climate heat index indicators above 40°C.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    lineHeight = 14.sp
                )
            }
            Switch(
                checked = tempPref.isEnabled,
                onCheckedChange = { onPrefUpdate("EXTREME_TEMP", it, tempPref.thresholdValue) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF22D3EE),
                    checkedTrackColor = Color(0xFF0C4A6E)
                )
            )
        }

        // Bangladesh Emergency contact details card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x1A22D3EE)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CrisisAlert, contentDescription = "Active info safety deck support", tint = Color(0xFF22D3EE))
                    Text(
                        text = "BANGLADESH MET PROTOCOLS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE)
                    )
                }

                Text(
                    text = "If severe cyclones emerge, please call the Bangladesh National Disaster Information Hotline 1098 or Fire Service at 999 immediately. Stay tuned to NOAA weather frequencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun RulePreferenceToggle(
    title: String,
    description: String,
    isEnabled: Boolean,
    value: Double,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onUpdate: (Boolean, Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { onUpdate(it, value) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF22D3EE),
                    checkedTrackColor = Color(0xFF0C4A6E)
                )
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
            lineHeight = 14.sp
        )

        if (isEnabled) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Warning limit:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(
                        text = "${value.toInt()}$unit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE)
                    )
                }
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onUpdate(true, it.toDouble()) },
                    valueRange = valueRange,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF0C4A6E),
                        thumbColor = Color(0xFF22D3EE)
                    )
                )
            }
        }
    }
}
