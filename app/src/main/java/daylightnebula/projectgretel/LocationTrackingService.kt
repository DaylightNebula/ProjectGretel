package daylightnebula.projectgretel

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.util.*

class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private var isTracking = false
    private var currentTrackId: UUID? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LocationTrackingChannel"
        private const val LOCATION_UPDATE_INTERVAL = 5 * 60 * 1000L // 5 minutes
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        createLocationRequest()
        createLocationCallback()
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

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                locationResult.lastLocation?.let { location ->
                    saveLocationToDatabase(location)
                }
            }
        }
    }

    fun startTracking(): UUID {
        if (isTracking) {
            return currentTrackId ?: UUID.randomUUID()
        }

        currentTrackId = UUID.randomUUID()
        isTracking = true

        DatabaseHelper
            .getInstance(this)
            .insertTrail(currentTrackId!!, "TRAIL_${System.currentTimeMillis()}")

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
            DatabaseHelper.getInstance(this).insertLocation(
                trackId,
                location.latitude,
                location.longitude,
                location.altitude,
                location.accuracy.toDouble(),
                location.time
            )
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
