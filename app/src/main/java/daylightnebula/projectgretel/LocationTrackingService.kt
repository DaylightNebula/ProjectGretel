package daylightnebula.projectgretel

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import daylightnebula.projectgretel.data.Trail
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            locationResult.lastLocation?.let { location ->
                saveLocationToDatabase(location)
            }
        }
    }

    private var isTracking = false
    private var saveTrack = false
    private var currentTrackId: UUID? = null

    var lastLongitude: Double = 0.0
        private set
    var lastLatitude: Double = 0.0
        private set
    var lastAltitude: Double = 0.0
        private set
    var lastAccuracy: Double = 0.0
        private set
    var lastTime: Long = 0
        private set
    var lastCount: Int = 0
        private set

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LocationTrackingChannel"
//        private const val LOCATION_UPDATE_INTERVAL = 5 * 60 * 1000L // 5 minutes
        private const val LOCATION_UPDATE_INTERVAL = 10 * 1000L // 10 seconds
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        createLocationRequest()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).build()
    }

    fun startTracking(trackId: UUID, saveTrack: Boolean): UUID {
        if (isTracking) return currentTrackId ?: UUID.randomUUID()

        this.currentTrackId = trackId
        this.isTracking = true
        this.saveTrack = saveTrack
        this.lastCount = 0

        val dateTimeFormat = SimpleDateFormat(
            "MM/dd/yy HH:mm:ss",
            resources.configuration.locales.get(0)
        )

        if (saveTrack) {
            DatabaseHelper
                .getInstance(this)
                .insertTrail(
                    Trail(
                        id = currentTrackId!!,
                        name = "TRAIL ${dateTimeFormat.format(Date(System.currentTimeMillis()))}",
                        time = System.currentTimeMillis(),
                        isFavorite = false
                    )
                )
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()

        return currentTrackId!!
    }

    fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        currentTrackId = null

        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun isTracking(): Boolean = isTracking
    fun isSaving(): Boolean = saveTrack
    fun getCurrentTrackId(): UUID? = currentTrackId

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveLocationToDatabase(location: Location) {
        currentTrackId?.let { trackId ->
            lastLongitude = location.longitude
            lastLatitude = location.latitude
            lastAltitude = location.altitude
            lastAccuracy = location.accuracy.toDouble()
            lastTime = location.time
            Log.d("LTS", "Found $lastLongitude $lastLatitude $lastAccuracy")

            if (!this.saveTrack) return@let

            // get current location
            val currentLocation = Trail.Location(
                owner = trackId,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy.toDouble(),
                time = location.time
            )

            // get db and last location
            val db = DatabaseHelper.getInstance(this)
            val isLastLocationToClose = db.getLastLocation(trackId)?.let { lastLocation ->
                val dist = lastLocation.distanceTo(currentLocation)
                Log.d("LTS", "Distance to last: $dist")
                dist < currentLocation.accuracy + lastLocation.accuracy
            } ?: false

            Log.d("LTS", "Adding location? ${!isLastLocationToClose}")

            // insert location and get last count
            /*if (!isLastLocationToClose) */lastCount = db.insertLocation(currentLocation)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracking Active")
            .setContentText("Track ID: $currentTrackId")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Continue tracking even when app is removed from recent apps
    }
}
