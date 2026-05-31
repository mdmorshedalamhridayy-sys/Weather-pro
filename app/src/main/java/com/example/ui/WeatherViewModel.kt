package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.WeatherApi
import com.example.data.database.AlertPreference
import com.example.data.database.TrackedLocation
import com.example.data.database.WeatherAlert
import com.example.data.database.WeatherDatabase
import com.example.data.model.WeatherState
import com.example.data.repository.PresetLocation
import com.example.data.repository.PresetLocations
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Active viewed location
    private val _activeLocationName = MutableStateFlow("Dhaka")
    val activeLocationName: StateFlow<String> = _activeLocationName.asStateFlow()

    private val _activeLatitude = MutableStateFlow(23.8103)
    val activeLatitude: StateFlow<Double> = _activeLatitude.asStateFlow()

    private val _activeLongitude = MutableStateFlow(90.4125)
    val activeLongitude: StateFlow<Double> = _activeLongitude.asStateFlow()

    // Screen content states
    private val _weatherStateResult = MutableStateFlow<Result<WeatherState>?>(null)
    val weatherStateResult: StateFlow<Result<WeatherState>?> = _weatherStateResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Alerts, saved locations, and user preference sync flows
    val trackedLocations: StateFlow<List<TrackedLocation>> = repository.trackedLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<WeatherAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertPreferences: StateFlow<List<AlertPreference>> = repository.alertPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search results lists
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<PresetLocation>> = _searchQuery
        .debounce(200)
        .map { query ->
            if (query.isBlank()) {
                emptyList()
            } else {
                PresetLocations.districts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.division.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Connectivity monitoring
    private val _isOnline = MutableStateFlow(isDeviceOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // GPS reading status messages
    private val _gpsStatusMessage = MutableStateFlow<String?>(null)
    val gpsStatusMessage: StateFlow<String?> = _gpsStatusMessage.asStateFlow()

    init {
        monitorNetworkConnectivity()
        // Fetch initially
        refreshActiveWeather()
    }

    /**
     * Re-fetch the active location's weather.
     */
    fun refreshActiveWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val name = _activeLocationName.value
            val lat = _activeLatitude.value
            val lon = _activeLongitude.value
            val result = repository.getWeather(name, lat, lon)
            _weatherStateResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Shifts active selected coordinates and triggers a fresh update.
     */
    fun selectLocation(name: String, latitude: Double, longitude: Double) {
        _activeLocationName.value = name
        _activeLatitude.value = latitude
        _activeLongitude.value = longitude
        _searchQuery.value = "" // clear search
        refreshActiveWeather()
    }

    /**
     * Request current device coordinate using GPS location services.
     */
    @SuppressLint("MissingPermission")
    fun requestDeviceLocation(onPermissionRequired: () -> Unit) {
        if (!hasLocationPermission()) {
            onPermissionRequired()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _gpsStatusMessage.value = "Detecting high precision GPS coordinates..."
            try {
                val cts = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            _gpsStatusMessage.value = "Location updated successfully."
                            selectLocation(
                                name = "My Location",
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        }
                    } else {
                        // try last known location as fallback
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    _gpsStatusMessage.value = "Using last known coordinates."
                                    selectLocation(
                                        name = "My Location",
                                        latitude = lastLoc.latitude,
                                        longitude = lastLoc.longitude
                                    )
                                }
                            } else {
                                _gpsStatusMessage.value = "GPS signal lost or unavailable."
                            }
                        }
                    }
                }.addOnFailureListener {
                    _gpsStatusMessage.value = "GPS acquisition failed: ${it.message}"
                }
            } catch (e: Exception) {
                _gpsStatusMessage.value = "Unable to read location coordinates."
            }
        }
    }

    fun clearGpsStatusMessage() {
        _gpsStatusMessage.value = null
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fineLocation == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                coarseLocation == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Database tracker utilities
     */
    fun addLocationToTrackList(name: String, lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val preset = PresetLocations.districts.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (preset != null) {
                repository.addTrackedLocation(preset)
            } else {
                repository.addTrackedLocationByCustom(name, lat, lon)
            }
        }
    }

    fun removeLocationFromTrackList(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeTrackedLocationByName(name)
        }
    }

    fun updateAlertPreference(type: String, enabled: Boolean, threshold: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePreference(AlertPreference(type, enabled, threshold))
        }
    }

    fun triggerSimulationWarning(locationName: String, title: String, message: String, intensity: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.simulateManualAlert(locationName, title, message, intensity)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Connectivity Observer to trigger "automatic updates on internet reconnect"
     */
    private fun monitorNetworkConnectivity() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(builder, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    Log.i("Connectivity", "Internet restored. Updating weather automatically.")
                    // Automatically trigger refresh on internet reconnect
                    refreshActiveWeather()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        } catch (e: Exception) {
            Log.e("Connectivity", "Callback registration failed", e)
        }
    }

    private fun isDeviceOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

class WeatherViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            val database = WeatherDatabase.getDatabase(application)
            val api = WeatherApi.create()
            val repository = WeatherRepository(
                weatherApi = api,
                weatherCacheDao = database.weatherCacheDao(),
                trackedLocationDao = database.trackedLocationDao(),
                weatherAlertDao = database.weatherAlertDao(),
                alertPreferenceDao = database.alertPreferenceDao(),
                context = application.applicationContext
            )
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
