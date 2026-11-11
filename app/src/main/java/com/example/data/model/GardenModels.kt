package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class HardscapeZone(
    val id: String = "",
    val type: String = "pathway",
    val label: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.2,
    val height: Double = 0.2,
    val materialNote: String = "",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type,
        "label" to label,
        "x" to x,
        "y" to y,
        "width" to width,
        "height" to height,
        "materialNote" to materialNote,
    )
}

@IgnoreExtraProperties
data class PlacedPlant(
    val id: String = "",
    val commonName: String = "",
    val botanicalName: String = "",
    val category: String = "Perennial",
    val sunNeed: String = "Full Sun",
    val waterNeed: String = "Moderate",
    val matureSpreadFeet: Double = 2.5,
    val matureHeightFeet: Double = 2.0,
    val bloomSeason: String = "Summer",
    val companionNote: String = "",
    val x: Double = 0.5,
    val y: Double = 0.5,
    val colorHex: String = "#4E7A5B",
    val localVisualPath: String = "",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "commonName" to commonName,
        "botanicalName" to botanicalName,
        "category" to category,
        "sunNeed" to sunNeed,
        "waterNeed" to waterNeed,
        "matureSpreadFeet" to matureSpreadFeet,
        "matureHeightFeet" to matureHeightFeet,
        "bloomSeason" to bloomSeason,
        "companionNote" to companionNote,
        "x" to x,
        "y" to y,
        "colorHex" to colorHex,
        "localVisualPath" to localVisualPath,
    )
}

@IgnoreExtraProperties
data class GardenDesign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val summary: String = "",
    val style: String = "",
    val widthFeet: Double = 20.0,
    val lengthFeet: Double = 20.0,
    val sunlight: String = "Full Sun",
    val soilType: String = "Loam",
    val climateZone: String = "Temperate",
    val hardscapeZones: List<HardscapeZone> = emptyList(),
    val plants: List<PlacedPlant> = emptyList(),
    val conceptVisualPath: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class GardenPreferences(
    val titleHint: String = "",
    val style: String = "Warm Cottage",
    val yardType: String = "Backyard",
    val widthFeet: Double = 24.0,
    val lengthFeet: Double = 18.0,
    val sunlight: String = "Full Sun",
    val soilType: String = "Loam",
    val climateZone: String = "Zone 7-8 (Temperate)",
    val includePathway: Boolean = true,
    val includeSeating: Boolean = true,
    val includeWaterFeature: Boolean = false,
    val includeEdibles: Boolean = false,
    val notes: String = "",
)
