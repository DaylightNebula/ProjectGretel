package daylightnebula.projectgretel.data

import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Trail(
    val id: UUID,
    val name: String,
    val time: Long,
    val isFavorite: Boolean
) {
    data class Location(
        val owner: UUID,
        val longitude: Double,
        val latitude: Double,
        val altitude: Double,
        val accuracy: Double,
        val time: Long
    ) {
        fun distanceTo(other: Location): Double {
            val earthRadius = 6371000.0 // Earth's radius in meters

            // Convert latitude and longitude from degrees to radians
            val lat1Rad = Math.toRadians(this.latitude)
            val lat2Rad = Math.toRadians(other.latitude)
            val deltaLat = Math.toRadians(other.latitude - this.latitude)
            val deltaLon = Math.toRadians(other.longitude - this.longitude)

            // Haversine formula
            val a = sin(deltaLat / 2).pow(2) +
                    cos(lat1Rad) * cos(lat2Rad) *
                    sin(deltaLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            // Horizontal distance
            val horizontalDistance = earthRadius * c

            // Altitude difference
            val altitudeDiff = other.altitude - this.altitude

            // 3D distance using Pythagorean theorem
            return sqrt(horizontalDistance.pow(2) + altitudeDiff.pow(2))
        }

        fun azimuthTo(target: Location): Double {
            val lat1 = this.latitude * 0.0174533
            val lat2 = target.latitude * 0.0174533
            val deltaLon = (target.longitude - this.longitude) * 0.0174533

            val y = sin(deltaLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)

            val bearing = atan2(y, x) * 57.2958

            // Normalize to 0-360 degrees
            return (bearing + 360) % 360
        }
    }
}
