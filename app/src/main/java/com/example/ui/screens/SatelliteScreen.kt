package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SatelliteMap

@Composable
fun SatelliteScreen(
    modifier: Modifier = Modifier
) {
    var showRadar by remember { mutableStateOf(true) }
    var showClouds by remember { mutableStateOf(true) }
    var showCycloneCone by remember { mutableStateOf(true) }
    var frameTimeline by remember { mutableStateOf(4f) } // Scrubber animation frame

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
    ) {
        // Fullscreen dynamic map with gestures
        SatelliteMap(
            modifier = Modifier.fillMaxSize(),
            showRadar = showRadar,
            showClouds = showClouds,
            showCycloneCone = showCycloneCone
        )

        // Floating Controllers Deck (Main Controls overlay)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xE6161B22), RoundedCornerShape(24.dp))
                .border(2.dp, Color(0xFF22D3EE).copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "METEOROLOGICAL SIMULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Bay of Bengal Control Station",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x3322D3EE))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FRAME 0${frameTimeline.toInt()} / 08",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Timeline Loop Scrubber
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Satellite Infrared Timeline Loop",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "-0${(8 - frameTimeline.toInt()) * 3} Hrs Ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
                Slider(
                    value = frameTimeline,
                    onValueChange = { frameTimeline = it },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF22D3EE),
                        inactiveTrackColor = Color(0xFF1E293B),
                        thumbColor = Color(0xFF22D3EE)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Layer toggle switches
            HorizontalDivider(color = Color(0xFF1E293B))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radar cloud density toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showRadar = !showRadar },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showRadar) Color(0xFF0C4A6E) else Color(0xFF1E293B)
                        )
                    ) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = "Radar toggle",
                            tint = if (showRadar) Color(0xFF22D3EE) else Color.LightGray
                        )
                    }
                    Text("Precip Radar", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }

                // Cloud cover overlay toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showClouds = !showClouds },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showClouds) Color(0xFF065F46) else Color(0xFF1E293B)
                        )
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Cloud toggle",
                            tint = if (showClouds) Color(0xFF34D399) else Color.LightGray
                        )
                    }
                    Text("Cloud Cover", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }

                // Cyclone projections toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showCycloneCone = !showCycloneCone },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showCycloneCone) Color(0xFF7F1D1D) else Color(0xFF1E293B)
                        )
                    ) {
                        Icon(
                            Icons.Default.Signpost,
                            contentDescription = "Projected paths toggle",
                            tint = if (showCycloneCone) Color(0xFFF87171) else Color.LightGray
                        )
                    }
                    Text("Impact Cone", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }
            }

            // Warnings advisory footer inside map controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x26EF4444))
                    .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Critical hazard advisory", tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                Text(
                    text = "Advisory: Cyclone hazard cells tracking towards Patuakhali coast. Storm surge 3.5m expected.",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
