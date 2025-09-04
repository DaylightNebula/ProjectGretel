package daylightnebula.projectgretel

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import daylightnebula.projectgretel.components.HomeComponent
import daylightnebula.projectgretel.components.TrackingComponent
import daylightnebula.projectgretel.ui.theme.ProjectGretelTheme

class MainActivity : ComponentActivity() {
    lateinit var databaseHelper: DatabaseHelper
    lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        databaseHelper = DatabaseHelper(this)
        database = databaseHelper.writableDatabase

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectGretelTheme {
                StateWrapper()
            }
        }
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
        State.Tracking -> TrackingComponent()
        State.Following -> TODO()
    }
}
