package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GardenModelsTest {

    @Test
    fun placedPlantToMap_containsExpectedFieldsWithoutNulls() {
        val plant = PlacedPlant(
            id = "p1",
            commonName = "Japanese Maple",
            botanicalName = "Acer palmatum",
            category = "Tree",
            sunNeed = "Part Shade",
            waterNeed = "Moderate",
            matureSpreadFeet = 10.0,
            matureHeightFeet = 12.0,
            bloomSeason = "Autumn Foliage",
            companionNote = "Graceful canopy over moss",
            x = 0.3,
            y = 0.4,
            colorHex = "#9E3B33",
            localVisualPath = "",
        )
        val map = plant.toMap()
        assertEquals("Japanese Maple", map["commonName"])
        assertEquals(10.0, map["matureSpreadFeet"])
        assertFalse(map.values.any { it == null })
    }

    @Test
    fun hardscapeZoneToMap_containsExpectedFields() {
        val zone = HardscapeZone(
            id = "z1",
            type = "water",
            label = "Stone Basin",
            x = 0.6,
            y = 0.2,
            width = 0.2,
            height = 0.2,
            materialNote = "Basalt stone basin",
        )
        val map = zone.toMap()
        assertEquals("water", map["type"])
        assertEquals("Stone Basin", map["label"])
    }
}
