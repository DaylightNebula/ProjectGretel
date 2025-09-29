package daylightnebula.projectgretel

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import java.util.UUID

class DatabaseHelper private constructor(
    context: Context
): SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    VERSION
) {
    companion object {
        const val VERSION = 1
        const val DB_NAME = "gretel.db"

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context).also { INSTANCE = it }
            }
        }

        val SCHEMAS = mapOf<Int, String>(
            1 to """
                CREATE TABLE IF NOT EXISTS trails (
                    id UUID NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    is_favorite BOOLEAN NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS trail_points (
                    trail_id UUID NOT NULL REFERENCES trails(id),
                    time TIMESTAMP NOT NULL,
                    longitude DECIMAL NOT NULL,
                    latitude DECIMAL NOT NULL,
                    altitude DECIMAL NOT NULL,
                    accuracy DECIMAL NOT NULL
                );
            """.trimIndent()
        )
    }

    private lateinit var db: SQLiteDatabase

    override fun onCreate(db: SQLiteDatabase) {
        this.db = db

        for (idx in 1 .. VERSION) {
            val schema = SCHEMAS[idx]
                ?: throw IllegalStateException("No schema for version $idx")
            db.execSQL(schema)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion >= newVersion)
            return

        for (idx in oldVersion .. newVersion) {
            val schema = SCHEMAS[idx]
                ?: throw IllegalStateException("No schema for version $idx")
            db.execSQL(schema)
        }
    }

    fun insertTrail(
        trackId: UUID,
        name: String,
        time: Long = System.currentTimeMillis(),
        isFavorite: Boolean = false
    ) {
        if (!this::db.isInitialized) {
            Log.e("DatabaseHelper", "DB not initialized!")
            return
        }

        this.db.execSQL("""
            INSERT INTO trails VALUES ('$trackId', "$name", $time $isFavorite);
        """.trimIndent())
    }

    fun insertLocation(
        trackId: UUID,
        longitude: Double,
        latitude: Double,
        altitude: Double,
        accuracy: Double,
        time: Long
    ) {
        if (!this::db.isInitialized) {
            Log.e("DatabaseHelper", "DB not initialized!")
            return
        }

        this.db.execSQL("""
            INSERT INTO trail_points VALUES ('$trackId', $time, $longitude, $latitude, $altitude, $accuracy);
        """.trimIndent())
    }
}