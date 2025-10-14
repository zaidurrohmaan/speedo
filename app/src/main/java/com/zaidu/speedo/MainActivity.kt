package com.zaidu.speedo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaidu.speedo.ui.theme.SpeedoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SpeedoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launcher untuk meminta izin lokasi
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] != true &&
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] != true) {
                    // Handle permission denial if needed
                }
            }

        // Meminta izin saat aplikasi dimulai
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            SpeedoTheme {
                val uiState by viewModel.uiState.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpeedoScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onStartClick = { viewModel.startTracking() },
                        onResumeClick = { viewModel.resumeTracking() },
                        onPauseClick = { viewModel.pauseTracking() },
                        onStopClick = { viewModel.stopTracking() },
                        onResetClick = { viewModel.resetTracking() }
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedoScreen(
    modifier: Modifier = Modifier,
    uiState: SpeedoUiState,
    onStartClick: () -> Unit,
    onResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Tampilan Kecepatan Utama
        SpeedDisplay(
            speed = uiState.speed,
            isRunning = uiState.isRunning,
            hasStarted = uiState.hasStarted
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Tampilan Jarak dan Waktu
        StatsDisplay(
            distance = uiState.distance,
            elapsedTime = uiState.elapsedTime,
            isRunning = uiState.isRunning,
            hasStarted = uiState.hasStarted
        )

        Spacer(modifier = Modifier.weight(1f))

        // Tombol Kontrol
        Controls(
            isRunning = uiState.isRunning,
            hasStarted = uiState.hasStarted,
            hasStopped = uiState.hasStopped,
            onStartClick = onStartClick,
            onPauseClick = onPauseClick,
            onStopClick = onStopClick,
            onResetClick = onResetClick,
            onResumeClick = onResumeClick
        )

        Spacer(modifier = Modifier.weight(0.2f))

        val title = if (!uiState.hasStarted) {
            if (uiState.hasStopped) {
                "Tap to reset"
            } else {
                "Tap to start"
            }
        } else {
            if (uiState.isRunning) {
                "Running"
            } else {
                "Paused"
            }
        }

        Text(
            text = title,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Composable
fun SpeedDisplay(speed: Float, isRunning: Boolean, hasStarted: Boolean) {
    // Konversi m/s ke Km/h
    val speedInKmh = (speed * 3.6).toInt()

    val animatedSpeed by animateIntAsState(
        targetValue = speedInKmh,
        animationSpec = tween(durationMillis = 900), // Durasi animasi
        label = "SpeedAnimation"
    )

    // Tentukan alpha: 1.0f (solid) jika berjalan, 0.5f (redup) jika paused
    val contentAlpha = if (hasStarted && !isRunning) 0.5f else 1.0f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$animatedSpeed",
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        )
        Text(
            text = "Km/h",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = contentAlpha)
        )
    }
}


@Composable
fun StatsDisplay(distance: Float, elapsedTime: Long, isRunning: Boolean, hasStarted: Boolean) {
    // Tentukan alpha: 1.0f (solid) jika berjalan, 0.5f (redup) jika paused
    val contentAlpha = if (hasStarted && !isRunning) 0.5f else 1.0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Tampilan Jarak
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.2f".format(distance / 1000), // meter ke km
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                text = "Distance (Km)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = contentAlpha)
            )
        }
        // Tampilan Waktu
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val hours = elapsedTime / 3600
            val minutes = (elapsedTime % 3600) / 60
            val seconds = elapsedTime % 60
            Text(
                text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                text = "Elapsed time",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = contentAlpha)
            )
        }
    }
}

@Composable
fun Controls(
    isRunning: Boolean,
    hasStarted: Boolean,
    hasStopped: Boolean,
    onStartClick: () -> Unit,
    onResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hasStarted) {
            if (hasStopped) CircularButton(icon = R.drawable.ic_reset, description = "Reset", onClick = onResetClick)
            else CircularButton(icon = R.drawable.ic_play, description = "Play", onClick = onStartClick)
        } else {
            if (isRunning) {
                CircularButton(icon = R.drawable.ic_pause, description = "Pause", onClick = onPauseClick)
            } else {
                CircularButton(icon = R.drawable.ic_resume, description = "Resume", onClick = onResumeClick)
            }
            CircularButton(icon = R.drawable.ic_stop, description = "Stop", onClick = onStopClick)
        }
    }
}

@Composable
fun CircularButton(icon: Int, description: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(100.dp)
            .border(1.dp, Color.Gray, CircleShape)
            .semantics {
                contentDescription = description
            },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),

    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
}

// --- Preview ---
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PreviewStoppedState() {
    SpeedoTheme {
        SpeedoScreen(
            uiState = SpeedoUiState(hasStarted = false),
            onStartClick = {}, onResumeClick = {}, onPauseClick = {}, onStopClick = {}, onResetClick = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PreviewRunningState() {
    SpeedoTheme {
        SpeedoScreen(
            uiState = SpeedoUiState(
                speed = 33.33f, // ~120 km/h
                distance = 12610f, // 12.61 km
                elapsedTime = 3779, // 01:02:59
                isRunning = true,
                hasStarted = true
            ),
            onStartClick = {}, onResumeClick = {}, onPauseClick = {}, onStopClick = {}, onResetClick = {}
        )
    }
}
