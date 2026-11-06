package com.example.motorapp.ui.screens

import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.R
import com.example.motorapp.domain.model.LeanDirection
import com.example.motorapp.domain.model.NavigationState
import com.example.motorapp.domain.model.PeerRider
import com.example.motorapp.domain.model.TelemetryData
import com.example.motorapp.ui.AppScreen
import com.example.motorapp.util.formatDecimal

@Composable
fun MainDashboardScreen(
    activeScreen: AppScreen,
    telemetryData: TelemetryData,
    navigationState: NavigationState,
    currentLocation: Location?,
    connectedPeers: Map<String, PeerRider>,
    isP2pSharing: Boolean,
    onSelectScreen: (AppScreen) -> Unit,
    onResetTelemetry: () -> Unit,
    onToggleP2pSharing: () -> Unit,
    onSetDestination: (Double, Double) -> Unit,
    onClearDestination: () -> Unit
) {
    if (activeScreen == AppScreen.MINIMALIST_HUD) {
        MinimalistHudScreen(
            telemetryData = telemetryData,
            navigationState = navigationState,
            onResetTelemetry = onResetTelemetry,
            onCloseHud = { onSelectScreen(AppScreen.DASHBOARD) }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF141416)) {
                NavigationBarItem(
                    selected = activeScreen == AppScreen.DASHBOARD,
                    onClick = { onSelectScreen(AppScreen.DASHBOARD) },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_telemetry)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF2C2C30),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeScreen == AppScreen.MAP_AND_RIDE,
                    onClick = { onSelectScreen(AppScreen.MAP_AND_RIDE) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF2C2C30),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onSelectScreen(AppScreen.MINIMALIST_HUD) },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_hud)) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeScreen) {
                AppScreen.DASHBOARD -> TelemetryDashboardView(
                    telemetryData = telemetryData,
                    onResetTelemetry = onResetTelemetry
                )
                AppScreen.MAP_AND_RIDE -> MapViewSection(
                    currentLocation = currentLocation,
                    connectedPeers = connectedPeers,
                    navigationState = navigationState,
                    isP2pSharing = isP2pSharing,
                    onToggleP2pSharing = onToggleP2pSharing,
                    onSetDestination = onSetDestination,
                    onClearDestination = onClearDestination
                )
                AppScreen.MINIMALIST_HUD -> {}
            }
        }
    }
}

@Composable
private fun TelemetryDashboardView(
    telemetryData: TelemetryData,
    onResetTelemetry: () -> Unit
) {
    val displaySpeed = if (telemetryData.gpsSpeedKmH > 0f) telemetryData.gpsSpeedKmH else telemetryData.calculatedSpeedKmH

    val directionLabel = when (telemetryData.leanDirection) {
        LeanDirection.LEFT -> stringResource(R.string.direction_left)
        LeanDirection.RIGHT -> stringResource(R.string.direction_right)
        LeanDirection.CENTER -> stringResource(R.string.direction_center)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MOTORAPP TELEMETRY",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
            border = BorderStroke(1.dp, Color(0xFF2C2C32))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LEAN ANGLE ($directionLabel)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA1A1AA)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${telemetryData.leanAngleDegrees.formatDecimal(1)}°",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MAX LEFT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "${telemetryData.maxLeftLeanDegrees.formatDecimal(1)}°",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF33333B))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MAX RIGHT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "${telemetryData.maxRightLeanDegrees.formatDecimal(1)}°",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
            border = BorderStroke(1.dp, Color(0xFF2C2C32))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "G-FORCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${telemetryData.gForce.formatDecimal(2)} G",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PEAK: ${telemetryData.maxGForce.formatDecimal(2)} G",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(52.dp)
                        .background(Color(0xFF33333B))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "SPEED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${displaySpeed.formatDecimal(0)} km/h",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GPS READY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }

        Button(
            onClick = onResetTelemetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222228)),
            border = BorderStroke(1.dp, Color(0xFF3A3A42))
        ) {
            Icon(
                imageVector = Icons.Default.CompassCalibration,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(
                text = "CALIBRATE / RESET (0°)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}
