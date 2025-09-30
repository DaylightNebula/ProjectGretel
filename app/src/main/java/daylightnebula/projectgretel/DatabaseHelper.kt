package daylightnebula.projectgretel

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import daylightnebula.projectgretel.data.Trail
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
                    is_favorite INTEGER NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS trail_points (
                    trail_id UUID NOT NULL REFERENCES trails(id) ON DELETE CASCADE,
                    time TIMESTAMP NOT NULL,
                    longitude DECIMAL NOT NULL,
                    latitude DECIMAL NOT NULL,
                    altitude DECIMAL NOT NULL,
                    accuracy DECIMAL NOT NULL
                );
            """.trimIndent()
        )
    }

    override fun onCreate(db: SQLiteDatabase) {
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

    fun insertTrail(trail: Trail) {
        val values = ContentValues().apply {
            put("id", trail.id.toString())
            put("name", trail.name)
            put("created_at", trail.time)
            put("is_favorite", if (trail.isFavorite) 1 else 0)
        }

        writableDatabase.insert("trails", null, values)
    }

    fun insertLocation(location: Trail.Location): Int {
        val values = ContentValues().apply {
            put("trail_id", location.owner.toString())
            put("time", location.time)
            put("longitude", location.longitude)
            put("latitude", location.latitude)
            put("altitude", location.altitude)
            put("accuracy", location.accuracy)
        }

        writableDatabase.insert("trail_points", null, values)

        // get cursor and count
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM trail_points WHERE trail_id = '${location.owner}'",
            arrayOf()
        )
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()

        return count
    }

    fun getAllTrails(): List<Trail> {
        val output = mutableListOf<Trail>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM trails",
            arrayOf()
        )

        if (cursor.moveToFirst()) {
            do {
                output.add(Trail(
                    id = UUID.fromString(cursor.getString(0)),
                    name = cursor.getString(1),
                    time = cursor.getLong(2),
                    isFavorite = cursor.getInt(3) == 1
                ))
            } while (cursor.moveToNext())
        }

        return output
    }

    fun getTrailLocations(trailId: UUID): List<Trail.Location> {
        val output = mutableListOf<Trail.Location>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM trail_points WHERE trail_id = '$trailId'",
            arrayOf()
        )

        if (cursor.moveToFirst()) {
            do {
                output.add(Trail.Location(
                    owner = UUID.fromString(cursor.getString(0)),
                    longitude = cursor.getDouble(2),
                    latitude = cursor.getDouble(3),
                    altitude = cursor.getDouble(4),
                    accuracy = cursor.getDouble(5),
                    time = cursor.getLong(1)
                ))
            } while (cursor.moveToNext())
        }

        return output
    }

    fun renameTrail(trailId: UUID, newName: String) {
        writableDatabase.execSQL("""
            UPDATE trails 
            SET name = "$newName" 
            WHERE id = '$trailId'
        """.trimIndent())
    }

    fun setFavorite(trailId: UUID, favorite: Boolean) {
        writableDatabase.execSQL("""
            UPDATE trails 
            SET is_favorite = ${if (favorite) "1" else "0"} 
            WHERE id = '$trailId'
        """.trimIndent())
    }

    fun deleteTrail(trailId: UUID) {
        writableDatabase.execSQL("DELETE FROM trail_points WHERE trail_id = '$trailId'")
        writableDatabase.execSQL("DELETE FROM trails WHERE id = '$trailId'")
    }
}