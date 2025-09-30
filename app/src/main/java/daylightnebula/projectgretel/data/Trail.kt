package daylightnebula.projectgretel.data

import java.util.UUID

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
    )
}
