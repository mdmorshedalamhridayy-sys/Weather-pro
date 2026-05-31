package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.TrackedLocation
import com.example.data.database.WeatherAlert
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherState
import com.example.data.repository.PresetLocation
import com.example.data.repository.PresetLocations
import com.example.ui.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeName by viewModel.activeLocationName.collectAsStateWithLifecycle()
    val activeLat by viewModel.activeLatitude.collectAsStateWithLifecycle()
    val activeLon by viewModel.activeLongitude.collectAsStateWithLifecycle()
    val weatherResult by viewModel.weatherStateResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val trackedLocations by viewModel.trackedLocations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val gpsStatus by viewModel.gpsStatusMessage.collectAsStateWithLifecycle()
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()

    var showDivisionMenu by remember { mutableStateOf(false) }
    var selectedDivisionFilter by remember { mutableStateOf("All Bangladesh") }

    // Toast GPS callback
    LaunchedEffect(gpsStatus) {
        gpsStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearGpsStatusMessage()
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.requestDeviceLocation {
                Toast.makeText(context, "Location permission is required for accurate GPS coordinates.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Location permission was denied. Falling back to default list.", Toast.LENGTH_SHORT).show()
        }
    }

    val divisions = listOf(
        "All Bangladesh", "Dhaka", "Chattogram", "Sylhet", "Khulna",
        "Rajshahi", "Barisal", "Rangpur", "Mymensingh", "Global Cities"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)) // Immersive Deep Dark Theme
            .statusBarsPadding()
    ) {
        // 1. App logo & Search / Options Row (Header)
        Surface(
            color = Color(0xFF161B22),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cyclone, contentDescription = "Weather Tracker Logo", tint = Color.White)
                        }
                        Text(
                            text = "WEATHER TRACKER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Online/Offline status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isOnline) Color(0x3322C55E) else Color(0x33EF4444))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444), RoundedCornerShape(50))
                            )
                            Text(
                                text = if (isOnline) "Live Sync" else "Cached Mode",
                                color = if (isOnline) Color(0xFF4ADE80) else Color(0xFFF87171),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // 2. Search Field + Division Filter Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search 64 Bangladesh districts & cities...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("location_search_bar"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0B0E14),
                            unfocusedContainerColor = Color(0xFF0B0E14),
                            disabledContainerColor = Color(0xFF0B0E14),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF22D3EE),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Division filtering button
                    Box {
                        Button(
                            onClick = { showDivisionMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(50.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (selectedDivisionFilter == "All Bangladesh") "Divisions" else selectedDivisionFilter,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter open", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showDivisionMenu,
                            onDismissRequest = { showDivisionMenu = false },
                            modifier = Modifier.background(Color(0xFF161B22))
                        ) {
                            divisions.forEach { div ->
                                DropdownMenuItem(
                                    text = { Text(div, color = Color.White) },
                                    onClick = {
                                        selectedDivisionFilter = div
                                        showDivisionMenu = false
                                        viewModel.updateSearchQuery("") // clear text search when selecting division
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search results popup
        AnimatedVisibility(
            visible = searchQuery.isNotEmpty() || selectedDivisionFilter != "All Bangladesh",
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                color = Color(0xFF161B22),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Header listing current query scope
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(8.dp, 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results" else "List: $selectedDivisionFilter",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                viewModel.updateSearchQuery("")
                                selectedDivisionFilter = "All Bangladesh"
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close selections", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Scroller list
                    val matchingLocations = remember(searchQuery, selectedDivisionFilter, searchResults) {
                        if (searchQuery.isNotEmpty()) {
                            searchResults
                        } else {
                            when (selectedDivisionFilter) {
                                "Global Cities" -> PresetLocations.districts.filter { it.isInternational }
                                "All Bangladesh" -> PresetLocations.districts.filter { !it.isInternational }
                                else -> PresetLocations.districts.filter { it.division.equals(selectedDivisionFilter, ignoreCase = true) }
                            }
                        }
                    }

                    if (matchingLocations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching locations found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(matchingLocations) { location ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectLocation(location.name, location.latitude, location.longitude)
                                            selectedDivisionFilter = "All Bangladesh"
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(location.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (location.division.isNotEmpty()) {
                                        Text("Division: ${location.division}", color = Color.Gray, fontSize = 12.sp)
                                    } else {
                                        Text("International Location", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF1E293B))
                            }
                        }
                    }
                }
            }
        }

        // Active Weather info body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0284C7))
                }
            } else {
                weatherResult?.let { result ->
                    result.fold(
                        onSuccess = { state ->
                            WeatherContentDetails(
                                state = state,
                                viewModel = viewModel,
                                trackedLocations = trackedLocations,
                                isOnline = isOnline,
                                alerts = alerts,
                                onGpsTrigger = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            OfflineErrorState(
                                activeName = activeName,
                                errorMsg = error.message ?: "Failed loading live weather data",
                                onGpsTrigger = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onRefresh = { viewModel.refreshActiveWeather() }
                            )
                        }
                    )
                } ?: run {
                    // No state loaded yet
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = { viewModel.refreshActiveWeather() }) {
                            Text("Fetch Forecast Data")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WeatherContentDetails(
    state: WeatherState,
    viewModel: WeatherViewModel,
    trackedLocations: List<TrackedLocation>,
    isOnline: Boolean,
    alerts: List<WeatherAlert>,
    onGpsTrigger: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isTracked = trackedLocations.any { it.locationName.equals(state.locationName, ignoreCase = true) }

    val todayForecast = state.dailyForecast.firstOrNull()
    val todayMax = todayForecast?.maxTemp?.toInt() ?: (state.temperature.toInt() + 2)
    val todayMin = todayForecast?.minTemp?.toInt() ?: (state.temperature.toInt() - 3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Critical Alert Banner (Styled after Immersive Theme HTML)
        val activeWarning = alerts.firstOrNull() ?: if (state.windSpeed > 35.0 || state.precipitation > 15.0) {
            WeatherAlert(
                locationName = state.locationName,
                title = "Storm Warning",
                description = "Severe cyclone rain and strong wind gusts of ${state.windSpeed.toInt()} km/h detected in area. Take cover.",
                intensity = "Severe",
                timestamp = System.currentTimeMillis()
            )
        } else {
            null
        }

        activeWarning?.let { alert ->
            val isExtreme = alert.intensity.equals("Extreme", ignoreCase = true)
            val bannerBg = if (isExtreme) Color(0x33EF4444) else Color(0x26D97706)
            val bannerBorder = if (isExtreme) Color(0xFFEF4444) else Color(0xFFF59E0B)
            val txtColor = if (isExtreme) Color(0xFFFCA5A5) else Color(0xFFFDE68A)

            Card(
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, bannerBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().clickable {
                    // Action on click
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bannerBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert logo",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = txtColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Core Summary Header: Matches the Immersive UI layout and design language exactly
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header details: Live Location status label with marker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Active Location Target",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "LIVE LOCATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.locationName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // GPS Locator Button
                        IconButton(
                            onClick = onGpsTrigger,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2B3544))
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Get GPS Location", tint = Color(0xFF22D3EE))
                        }

                        // Save Tracker Button
                        IconButton(
                            onClick = {
                                if (isTracked) {
                                    viewModel.removeLocationFromTrackList(state.locationName)
                                } else {
                                    viewModel.addLocationToTrackList(state.locationName, state.latitude, state.longitude)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isTracked) Color(0xFF0C4A6E) else Color(0xFF2B3544))
                        ) {
                            Icon(
                                imageVector = if (isTracked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isTracked) "Remove saved location" else "Track location",
                                tint = if (isTracked) Color(0xFFEF4444) else Color.White
                            )
                        }
                    }
                }

                // Coordinates + Time detail text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Coord: ${getDecimalFormat(state.latitude)}°N, ${getDecimalFormat(state.longitude)}°E",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    val updatedStr = remember(state.lastUpdated) {
                        val sdf = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault())
                        sdf.format(Date(state.lastUpdated))
                    }
                    Text(
                        text = "Updated: $updatedStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                // Current Temperature Hero Section: Clean, Light Display, atmospheric glow background
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "${state.temperature.toInt()}",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
                                color = Color.White
                            )
                            Text(
                                text = "°C",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF22D3EE),
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                        Text(
                            text = "${state.weatherCondition} • H: ${todayMax}° L: ${todayMin}°",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Feels like ${state.apparentTemperature.toInt()}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Adaptive glow background for the Weather Icon
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(Color(0x1F22D3EE), RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getWeatherIcon(state.weatherCode),
                            contentDescription = state.weatherCondition,
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }
        }

        // Sub Grid metrics: Humidity, Wind speed, Precipitation, Storm Risk Indexes
        Text(
            text = "Current Conditions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Humidity",
                    value = "${state.humidity.toInt()}%",
                    explanation = "Water vapor relative volume",
                    icon = Icons.Outlined.WaterDrop,
                    iconColor = Color(0xFF38BDF8)
                )
                MetricCard(
                    title = "Precipitation",
                    value = "${state.precipitation} mm",
                    explanation = "Direct local rainfall level",
                    icon = Icons.Outlined.Umbrella,
                    iconColor = Color(0xFF60A5FA)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Wind Speed",
                    value = "${state.windSpeed} km/h",
                    explanation = "Speed: ${getWindDirectionName(state.windDirection)}",
                    icon = Icons.Outlined.Air,
                    iconColor = Color(0xFF34D399)
                )
                MetricCard(
                    title = "Storm Index",
                    value = getStormRiskLabel(state.windSpeed, state.precipitation),
                    explanation = "Cyclone storm probability",
                    icon = Icons.Outlined.Cyclone,
                    iconColor = Color(0xFFF87171)
                )
            }
        }

        // Horizontal 24-hr layout forecast list
        Text(
            text = "Hourly Forecast (Next 24h)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(state.hourlyForecast) { hour ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.width(80.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = hour.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Icon(
                            imageVector = getWeatherIcon(hour.weatherCode),
                            contentDescription = "Forecast visual hour",
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "${hour.temperature.toInt()}°",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.Air, contentDescription = "Wind speed hourly", tint = Color(0xFF64748B), modifier = Modifier.size(10.dp))
                            Text(
                                text = "${hour.windSpeed.toInt()}k",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // 7-day extended forecasts vertical layouts
        Text(
            text = "Extended Forecast (7 Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.dailyForecast.forEachIndexed { index, day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (index == 0) "Today" else day.date,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.width(90.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(100.dp)
                        ) {
                            Icon(
                                imageVector = getWeatherIcon(day.weatherCode),
                                contentDescription = "Weather daily summary visual",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = translateWeatherCodeLocal(day.weatherCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${day.maxTemp.toInt()}°C",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${day.minTemp.toInt()}°C",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    if (index < state.dailyForecast.lastIndex) {
                        HorizontalDivider(color = Color(0xFF1E293B))
                    }
                }
            }
        }

        // Active Saved/Tracked Locations scraper (Shortcuts)
        if (trackedLocations.size > 1) {
            Text(
                text = "My Tracked Shortcuts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trackedLocations.forEach { loc ->
                    val isActiveSelection = loc.locationName.equals(state.locationName, ignoreCase = true)
                    InputChip(
                        selected = isActiveSelection,
                        onClick = { viewModel.selectLocation(loc.locationName, loc.latitude, loc.longitude) },
                        label = { Text(loc.locationName, color = Color.White) },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.removeLocationFromTrackList(loc.locationName) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Delete tracked tracker", tint = Color.LightGray)
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = Color(0xFF161B22),
                            selectedContainerColor = Color(0xFF0C4A6E)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    explanation: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1A22D3EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun OfflineErrorState(
    activeName: String,
    errorMsg: String,
    onGpsTrigger: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x1AEF4444), RoundedCornerShape(100)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = "Offline Cache fail", tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                }

                Text(
                    text = "Offline Mode Failure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "We couldn't connect to internet, and there is no cached weather state saved in the database for $activeName yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFF1E293B))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onGpsTrigger,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use GPS Device coordinates")
                        Spacer(Modifier.width(6.dp))
                        Text("Active GPS")
                    }

                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh offline state retry")
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}

// Utility formatting and conversions
private fun getDecimalFormat(value: Double): String {
    return String.format(Locale.US, "%.4f", value)
}

fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Outlined.LightMode
        1, 2 -> Icons.Outlined.CloudQueue
        3 -> Icons.Outlined.Cloud
        45, 48 -> Icons.Outlined.FilterDrama
        51, 53, 55, 56, 57 -> Icons.Outlined.WaterDrop
        61, 63 -> Icons.Outlined.Umbrella
        65 -> Icons.Outlined.Thunderstorm
        80, 81, 82 -> Icons.Outlined.Grain
        95, 96, 99 -> Icons.Outlined.Cyclone
        else -> Icons.Outlined.QuestionMark
    }
}

fun getWeatherIconColor(code: Int): Color {
    return when (code) {
        0 -> Color(0xFFFBBF24) // Yellow
        1, 2 -> Color(0xFF93C5FD) // Sky Blue
        3 -> Color(0xFFCBD5E1) // Gray Blue
        51, 53, 55, 61, 63, 80, 81 -> Color(0xFF38BDF8) // Light Blue
        65 -> Color(0xFF60A5FA) // Med Blue
        95, 96, 99 -> Color(0xFFEF4444) // Coral Red
        else -> Color.White
    }
}

fun getWindDirectionName(degree: Double): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
    val index = ((degree % 360) / 45).toInt()
    return directions[index]
}

fun getStormRiskLabel(windSpeed: Double, precipitation: Double): String {
    return when {
        windSpeed >= 50.0 || precipitation >= 25.0 -> "EXTREME"
        windSpeed >= 35.0 || precipitation >= 10.0 -> "HIGH"
        windSpeed >= 20.0 || precipitation >= 3.0 -> "MODERATE"
        else -> "LOW"
    }
}

fun translateWeatherCodeLocal(code: Int): String {
    return when (code) {
        0 -> "Sunny"
        1, 2, 3 -> "Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Heavy Rain"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Storms"
        else -> "Overcast"
    }
}
