package daylightnebula.projectgretel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import daylightnebula.projectgretel.LocationTrackingService
import daylightnebula.projectgretel.ui.theme.Purple40
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun TrackingComponent(
    locationService: LocationTrackingService,
    onEndTracking: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6000) // 3 seconds
            currentTime = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Actively Tracking!")
            Text("Please leave the app running in the background while your enjoy your travels!")
            Spacer(Modifier.height(30.dp))
            Text("GPS ${locationService.lastLatitude} x ${locationService.lastLongitude}")
            Text("Accuracy ${locationService.lastAccuracy}m")
            Text("Time ${Date(currentTime)}")
            Text("Count ${locationService.lastCount}")
        }

        Button(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonColors(
                containerColor = Purple40,
                contentColor = Color.White,
                disabledContentColor = Color.Black,
                disabledContainerColor = Color.Red
            ),
            onClick = onEndTracking
        ) {
            Text("End Tracking")
        }
    }
}