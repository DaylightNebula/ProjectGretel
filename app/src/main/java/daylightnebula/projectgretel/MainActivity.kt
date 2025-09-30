package daylightnebula.projectgretel

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import daylightnebula.projectgretel.components.HomeComponent
import daylightnebula.projectgretel.components.TrackingComponent
import daylightnebula.projectgretel.ui.theme.ProjectGretelTheme

class MainActivity : ComponentActivity() {
    private lateinit var locationService: LocationTrackingService
    private var locationServiceBound = false

    enum class State {
        Home,
        Tracking,
        Following
    }

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocationTrackingService.LocalBinder
            locationService = binder.getService()
            locationServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            locationServiceBound = false
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocation()
                } else {
                    requestNotificationPermission()
                }
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                Toast.makeText(this, "Approximate location access granted.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // No location access granted.
                Toast.makeText(this, "Location permission is required for tracking.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val backgroundLocationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestNotificationPermission()
        } else {
            Toast.makeText(this, "Background location permission is required for continuous tracking.", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is required for foreground service.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setup UI
        enableEdgeToEdge()
        setContent {
            ProjectGretelTheme {
                StateWrapper()
            }
        }

        checkPermissions()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, LocationTrackingService::class.java).also { intent ->
            bindService(intent, locationServiceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(locationServiceConnection)
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            locationPermissionRequest.launch(permissions.toTypedArray())
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocation()
            } else {
                requestNotificationPermission()
            }
        }
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                backgroundLocationPermissionRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                requestNotificationPermission()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Composable
    fun StateWrapper() {
        var currentState by remember {
            val initialState =
                if (locationServiceBound && locationService.isTracking()) State.Tracking
                else State.Home

            mutableStateOf(initialState)
        }

        when (currentState) {
            State.Home -> HomeComponent(
                context = this,
                onStartTrail = {
                    Log.d("MainActivity", "Open start trail!")
                    locationService.startTracking()
                    currentState = State.Tracking
                }
            )
            State.Tracking -> TrackingComponent(
                locationService = locationService,
                onEndTracking = {
                    Log.d("MainActivity", "Ending trail!")
                    locationService.stopTracking()
                    currentState = State.Home
                }
            )
            State.Following -> TODO()
        }
    }
}
