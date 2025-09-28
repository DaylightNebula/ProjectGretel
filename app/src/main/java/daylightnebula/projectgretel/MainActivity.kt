package daylightnebula.projectgretel

import android.Manifest
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.liftric.kvault.KVault
import daylightnebula.projectgretel.components.HomeComponent
import daylightnebula.projectgretel.components.TrackingComponent
import daylightnebula.projectgretel.ui.theme.ProjectGretelTheme

val CURRENT_TRAIL_KV_ID = "gretel.kv.current_trail"
val LOCATION_PERMISSION_REQUEST_CODE = 1

class MainActivity : ComponentActivity() {
    val store = KVault(this, "user.kv")

    var lastGpsTime: Long = 0
    var lastGpsAccuracy: Float = 0f
    var lastLatitude: Double = 0.0
    var lastLongitude: Double = 0.0

    lateinit var databaseHelper: DatabaseHelper
    lateinit var database: SQLiteDatabase

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        databaseHelper = DatabaseHelper(this)
        database = databaseHelper.writableDatabase

        super.onCreate(savedInstanceState)

        // setup UI
        enableEdgeToEdge()
        setContent {
            ProjectGretelTheme {
                StateWrapper()
            }
        }

        // setup fused location client
        fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)

        // setup location request
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(10000)
            .build();

        // create location callback
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                    lastGpsTime = location.time
                    lastGpsAccuracy = location.accuracy
                }
            }
        }

        // setup permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startLocationUpdates()
            else
                Toast.makeText(
                    this,
                    "Location permission required",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermissions())
            startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

enum class State {
    Home,
    Tracking,
    Following
}

@Composable
fun StateWrapper() {
    var currentState by remember { mutableStateOf(State.Home) }

    when (currentState) {
        State.Home -> HomeComponent(
            onStartTrail = { println("Open start trail!"); currentState = State.Tracking; }
        )
        State.Tracking -> TrackingComponent(
            onEndTracking = { println("Stop trail!"); currentState = State.Home; }
        )
        State.Following -> TODO()
    }
}
