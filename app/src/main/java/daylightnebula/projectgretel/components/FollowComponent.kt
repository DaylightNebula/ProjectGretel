package daylightnebula.projectgretel.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import daylightnebula.projectgretel.DatabaseHelper
import daylightnebula.projectgretel.LocationTrackingService
import daylightnebula.projectgretel.data.Trail
import daylightnebula.projectgretel.ui.theme.Purple40
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FollowComponent(
    context: Context,
    locationService: LocationTrackingService,
    trackId: UUID,
    onReturn: () -> Unit
) {
    val context = LocalContext.current
    val db = DatabaseHelper.getInstance(context)
    var pointIndex by remember { mutableIntStateOf(0) }
//    var trackingPoint by remember {
//        val trail = db.getTrailLocations(trackId)
//        mutableStateOf(trail[0]) // todo what in no points in trail
//    }

    val points = db.getTrailLocations(trackId)
    val trackingPoint =
        if (pointIndex < 0) points.first()
        else if (pointIndex >= points.size) points.last()
        else points[pointIndex]

    var targetAzimuth by remember {
        val azimuth = Trail.Location(
            owner = trackId,
            longitude = locationService.lastLongitude,
            latitude = locationService.lastLatitude,
            altitude = locationService.lastAltitude,
            accuracy = locationService.lastAccuracy,
            time = locationService.lastTime
        ).azimuthTo(trackingPoint)
        mutableDoubleStateOf(azimuth)
    }

    var azimuth by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableStateOf("N") }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                    }
                }

                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)

                val success = SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    accelerometerReading,
                    magnetometerReading
                )

                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    // Convert radians to degrees and normalize to 0-360
                    val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    azimuth = (azimuthDegrees + 360) % 360

                    direction = when (azimuth) {
                        in 337.5f..360.0f, in 0.0f..22.5f -> "N"
                        in 22.5f..67.5f -> "NE"
                        in 67.5f..112.5f -> "E"
                        in 112.5f..157.5f -> "SE"
                        in 157.5f..202.5f -> "S"
                        in 202.5f..247.5f -> "SW"
                        in 247.5f..292.5f -> "W"
                        in 292.5f..337.5f -> "NW"
                        else -> "N"
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            sensorEventListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    Button(
        modifier = Modifier
            .padding(
                top = 100.dp,
                bottom = 10.dp,
                start = 10.dp,
                end = 10.dp
            )
            .fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonColors(
            containerColor = Purple40,
            contentColor = Color.White,
            disabledContentColor = Color.Black,
            disabledContainerColor = Color.Red
        ),
        onClick = onReturn
    ) {
        Text("End Follow")
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var drawAzimuth = azimuth - targetAzimuth
            drawAzimuth = (drawAzimuth + 360) % 360

            val currentLocation = Trail.Location(
                owner = trackId,
                longitude = locationService.lastLongitude,
                latitude = locationService.lastLatitude,
                altitude = locationService.lastAltitude,
                accuracy = locationService.lastAccuracy,
                time = locationService.lastTime
            )
            val dist = currentLocation.distanceTo(trackingPoint)

            if (dist < trackingPoint.accuracy && pointIndex < points.size - 1)
                pointIndex++

            CompassView(azimuth = drawAzimuth)
            Spacer(modifier = Modifier.height(8.dp))
            if (pointIndex < points.size && dist > trackingPoint.accuracy) {
                Text(
                    text = "To Next Point: %.0fm\nAccuracy: %.1fm".format(
                        dist,
                        trackingPoint.accuracy + currentLocation.accuracy
                    ),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            } else {
                Text(
                    text = "Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun CompassView(azimuth: Double) {
    Canvas(
        modifier = Modifier.size(300.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val radius = minOf(canvasWidth, canvasHeight) / 2 - 40f

        // Outer circle
        drawCircle(
            color = Color(0xFF2D2D44),
            radius = radius,
            center = center,
            style = Stroke(width = 4f)
        )

        // Inner circle
        drawCircle(
            color = Color(0xFF3D3D54),
            radius = radius - 30f,
            center = center,
            style = Stroke(width = 2f)
        )

        rotate((-azimuth).toFloat(), pivot = center) {
            // Tick marks for degrees
            for (i in 0 until 360 step 10) {
                val rad = Math.toRadians(i.toDouble())
                val startRadius = if (i % 30 == 0) radius - 25f else radius - 15f

                val startX = center.x + startRadius * sin(rad).toFloat()
                val startY = center.y - startRadius * cos(rad).toFloat()
                val endX = center.x + radius * sin(rad).toFloat()
                val endY = center.y - radius * cos(rad).toFloat()

                drawLine(
                    color = Color(0xFF666666),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 30 == 0) 3f else 1f
                )
            }

            // North needle (fixed, pointing up)
            drawLine(
                color = Color(0xFFFF5555),
                start = center,
                end = Offset(center.x, center.y - radius + 50f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            // South needle (fixed, pointing down)
            drawLine(
                color = Color(0xFF666666),
                start = center,
                end = Offset(center.x, center.y + radius - 50f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            // Center dot
            drawCircle(
                color = Color(0xFF666666),
                radius = 8f,
                center = center
            )
        }
    }
}

fun getDirection(azimuth: Double): String {
    val normalized = normalizeAzimuth(azimuth)
    return when {
        normalized >= 337.5 || normalized < 22.5 -> "N"
        normalized >= 22.5 && normalized < 67.5 -> "NE"
        normalized >= 67.5 && normalized < 112.5 -> "E"
        normalized >= 112.5 && normalized < 157.5 -> "SE"
        normalized >= 157.5 && normalized < 202.5 -> "S"
        normalized >= 202.5 && normalized < 247.5 -> "SW"
        normalized >= 247.5 && normalized < 292.5 -> "W"
        normalized >= 292.5 && normalized < 337.5 -> "NW"
        else -> "N"
    }
}

fun normalizeAzimuth(azimuth: Double): Double {
    return (azimuth + 360) % 360
}

fun lowPassFilter(input: FloatArray, output: FloatArray?): FloatArray {
    val alpha = 0.15f // Smoothing factor (0 < alpha < 1)

    if (output == null) return input

    for (i in input.indices) {
        output[i] = output[i] + alpha * (input[i] - output[i])
    }

    return output
}

fun smoothAzimuth(lastAzimuth: Double, newAzimuth: Double): Double {
    var diff = newAzimuth - lastAzimuth

    // Handle 360/0 degree boundary
    if (diff > 180) {
        diff -= 360
    } else if (diff < -180) {
        diff += 360
    }

    return (lastAzimuth + diff * 0.3f + 360) % 360
}
