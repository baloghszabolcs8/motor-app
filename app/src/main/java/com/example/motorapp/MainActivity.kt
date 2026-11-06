package com.example.motorapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.motorapp.ui.TelemetryViewModel
import com.example.motorapp.ui.screens.MainDashboardScreen
import com.example.motorapp.ui.theme.MotorappTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    private val viewModel: TelemetryViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set custom compliant User-Agent for OpenStreetMap tile servers to avoid 403 blocks
        Configuration.getInstance().userAgentValue = "MotorAppTelemetry/1.0 (https://github.com/motorapp/motorapp)"

        enableEdgeToEdge()
        checkAndRequestPermissions()

        setContent {
            MotorappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val activeScreen by viewModel.activeScreen.collectAsState()
                    val telemetryData by viewModel.telemetryState.collectAsState()
                    val navigationState by viewModel.navigationState.collectAsState()
                    val currentLocation by viewModel.currentLocation.collectAsState()
                    val connectedPeers by viewModel.connectedPeers.collectAsState()
                    val isP2pSharing by viewModel.isP2pSharing.collectAsState()

                    MainDashboardScreen(
                        activeScreen = activeScreen,
                        telemetryData = telemetryData,
                        navigationState = navigationState,
                        currentLocation = currentLocation,
                        connectedPeers = connectedPeers,
                        isP2pSharing = isP2pSharing,
                        onSelectScreen = { viewModel.setActiveScreen(it) },
                        onResetTelemetry = { viewModel.resetTelemetry() },
                        onToggleP2pSharing = { viewModel.toggleP2pRideShare() },
                        onSetDestination = { lat, lng -> viewModel.setDestination(lat, lng) },
                        onClearDestination = { viewModel.clearDestination() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestPermissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}
