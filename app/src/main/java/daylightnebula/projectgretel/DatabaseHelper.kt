package daylightnebula.projectgretel

import android.content.Context
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(
    val context: Context
): SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    VERSION
) {
    companion object {
        const val VERSION = 1;
        const val DB_NAME = "gretel.db";

        val SCHEMAS = mapOf<Int, String>(
            1 to """
                CREATE TABLE IF NOT EXISTS trails (
                    id UUID NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    is_favorite BOOLEAN NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS trail_points (
                    trail_id UUID NOT NULL,
                    time TIMESTAMP NOT NULL,
                    latitude DECIMAL NOT NULL,
                    longitude DECIMAL NOT NULL,
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
}