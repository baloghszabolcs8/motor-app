package com.example.motorapp.ui.screens

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.R
import com.example.motorapp.domain.model.NavigationState
import com.example.motorapp.domain.model.TelemetryData
import com.example.motorapp.domain.model.TurnManeuver
import com.example.motorapp.util.formatDecimal
import com.example.motorapp.util.formatDistance

@Composable
fun MinimalistHudScreen(
    telemetryData: TelemetryData,
    navigationState: NavigationState,
    onResetTelemetry: () -> Unit,
    onCloseHud: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }

    val speed = if (telemetryData.gpsSpeedKmH > 0f) telemetryData.gpsSpeedKmH else telemetryData.calculatedSpeedKmH

    val (maneuverSymbol, maneuverText) = when (navigationState.nextManeuver) {
        TurnManeuver.LEFT -> "←" to "TURN LEFT"
        TurnManeuver.SLIGHT_LEFT -> "↖" to "BEAR LEFT"
        TurnManeuver.SHARP_LEFT -> "⮌" to "SHARP LEFT"
        TurnManeuver.RIGHT -> "→" to "TURN RIGHT"
        TurnManeuver.SLIGHT_RIGHT -> "↗" to "BEAR RIGHT"
        TurnManeuver.SHARP_RIGHT -> "⮱" to "SHARP RIGHT"
        TurnManeuver.STRAIGHT -> "↑" to "CONTINUE STRAIGHT"
        TurnManeuver.U_TURN -> "↶" to "U-TURN"
        TurnManeuver.ARRIVED -> "🏁" to "ARRIVED"
        TurnManeuver.NONE -> "" to ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (navigationState.hasActiveDestination) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$maneuverSymbol  $maneuverText",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "in ${navigationState.distanceToNextTurnMeters.formatDistance()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.label_lean_angle).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${telemetryData.leanAngleDegrees.formatDecimal(1)}°",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${telemetryData.maxLeftLeanDegrees.formatDecimal(1)}°   MAX   ${telemetryData.maxRightLeanDegrees.formatDecimal(1)}°",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MUSIC",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = {
                            sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Pause",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = { sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT) },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${speed.formatDecimal(0)} km/h",
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(
                onClick = onResetTelemetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(44.dp)
                    .border(1.dp, Color.White, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "RESET / TARE (0°)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "TAP HERE TO EXIT HUD",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.clickable { onCloseHud() }
            )
        }
    }
}

private fun sendMediaKey(context: Context, keyCode: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    audioManager?.let {
        val now = SystemClock.uptimeMillis()
        it.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        it.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }
}
