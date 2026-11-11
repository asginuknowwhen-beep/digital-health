package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.model.GardenDesign
import com.example.data.model.GardenPreferences
import com.example.data.model.HardscapeZone
import com.example.data.model.PlacedPlant
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GardenAiRepository(
    private val appContext: Context,
) {
    private val gardenLayoutSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Inspiring 2-5 word title for the garden design"),
            "summary" to Schema.string(description = "2-3 sentence landscape architecture and planting overview"),
            "hardscapeZones" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "type" to Schema.enumeration(
                            values = listOf("pathway", "patio", "water", "lawn", "deck", "bed"),
                            description = "Hardscape zone category",
                        ),
                        "label" to Schema.string(),
                        "x" to Schema.double(description = "Normalized left coordinate 0.0..0.9"),
                        "y" to Schema.double(description = "Normalized top coordinate 0.0..0.9"),
                        "width" to Schema.double(description = "Normalized width 0.08..0.85"),
                        "height" to Schema.double(description = "Normalized height 0.08..0.85"),
                        "materialNote" to Schema.string(description = "Suggested stone, gravel, timber, or paving material"),
                    )
                ),
                description = "2 to 5 structured hardscape zones forming the garden skeleton",
            ),
            "plants" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(),
                        "commonName" to Schema.string(),
                        "botanicalName" to Schema.string(),
                        "category" to Schema.enumeration(
                            values = listOf("Tree", "Shrub", "Perennial", "Ornamental Grass", "Climber", "Groundcover", "Edible"),
                        ),
                        "sunNeed" to Schema.string(),
                        "waterNeed" to Schema.string(),
                        "matureSpreadFeet" to Schema.double(description = "Mature canopy spread diameter in feet (1.0..18.0)"),
                        "matureHeightFeet" to Schema.double(description = "Mature height in feet (0.5..25.0)"),
                        "bloomSeason" to Schema.string(),
                        "companionNote" to Schema.string(description = "Why this plant works well here and which neighbors complement it"),
                        "x" to Schema.double(description = "Normalized center X coordinate 0.08..0.92"),
                        "y" to Schema.double(description = "Normalized center Y coordinate 0.08..0.92"),
                        "colorHex" to Schema.string(description = "Botanical canopy hex color e.g. #5A8F6B or #8C6BB1"),
                    )
                ),
                description = "7 to 12 thoughtfully spaced plants arranged harmoniously around hardscape zones",
            ),
        )
    )

    private val companionSuggestionSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "commonName" to Schema.string(),
                        "botanicalName" to Schema.string(),
                        "category" to Schema.enumeration(
                            values = listOf("Tree", "Shrub", "Perennial", "Ornamental Grass", "Climber", "Groundcover", "Edible"),
                        ),
                        "sunNeed" to Schema.string(),
                        "waterNeed" to Schema.string(),
                        "matureSpreadFeet" to Schema.double(),
                        "matureHeightFeet" to Schema.double(),
                        "bloomSeason" to Schema.string(),
                        "companionNote" to Schema.string(),
                        "colorHex" to Schema.string(),
                    )
                ),
                description = "3 curated plant alternatives or companion additions matching the garden conditions",
            )
        )
    )

    private val layoutModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = gardenLayoutSchema
            },
        )
    }

    private val companionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = companionSuggestionSchema
            },
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            },
        )
    }

    suspend fun generateGardenLayout(
        prefs: GardenPreferences,
        referenceYardBitmap: Bitmap? = null,
    ): Result<GardenDesign> {
        return try {
            val promptText = buildLayoutPrompt(prefs, referenceYardBitmap != null)
            val response = withTimeout(AI_TIMEOUT_MS) {
                if (referenceYardBitmap != null) {
                    layoutModel.generateContent(
                        content {
                            image(referenceYardBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    layoutModel.generateContent(promptText)
                }
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty layout response."))
            val parsed = parseGardenDesignJson(rawJson, prefs)
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Layout generation timed out", e)
            Result.failure(IllegalStateException("Layout generation timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Layout generation quota exceeded", e)
            Result.failure(IllegalStateException("AI quota exceeded. Please try again shortly."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Layout generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun suggestCompanionPlants(
        design: GardenDesign,
        focusNote: String,
    ): Result<List<PlacedPlant>> {
        return try {
            val existingNames = design.plants.joinToString(", ") { it.commonName }
            val prompt = """
                You are a master landscape horticulturist. Suggest 3 distinct companion or replacement plants for a ${design.style} garden (${design.widthFeet}x${design.lengthFeet} ft) with ${design.sunlight} exposure, ${design.soilType} soil, and ${design.climateZone} climate.
                Existing plants in the layout: $existingNames.
                User focus request: ${focusNote.ifBlank { "Add seasonal color, fragrance, and layered height contrast." }}
                Avoid duplicating plants already listed. Return 3 curated options with realistic mature spread/height and harmonious hex canopy colors.
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                companionModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Model returned empty plant suggestions."))
            val root = JSONObject(rawJson)
            val array = root.optJSONArray("suggestions") ?: JSONArray()
            val plants = (0 until array.length()).map { idx ->
                val obj = array.getJSONObject(idx)
                PlacedPlant(
                    id = "plant_${UUID.randomUUID().toString().take(8)}",
                    commonName = obj.optString("commonName", "Garden Perennial"),
                    botanicalName = obj.optString("botanicalName", ""),
                    category = obj.optString("category", "Perennial"),
                    sunNeed = obj.optString("sunNeed", design.sunlight),
                    waterNeed = obj.optString("waterNeed", "Moderate"),
                    matureSpreadFeet = obj.optDouble("matureSpreadFeet", 3.0).coerceIn(1.0, 18.0),
                    matureHeightFeet = obj.optDouble("matureHeightFeet", 2.5).coerceIn(0.5, 25.0),
                    bloomSeason = obj.optString("bloomSeason", "Summer"),
                    companionNote = obj.optString("companionNote", ""),
                    x = 0.5,
                    y = 0.5,
                    colorHex = sanitizeHex(obj.optString("colorHex", "#52796F")),
                    localVisualPath = "",
                )
            }
            Result.success(plants)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Companion suggestion timed out", e)
            Result.failure(IllegalStateException("Plant suggestion timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Companion suggestion failed", e)
            Result.failure(e)
        }
    }

    suspend fun generatePlantVisual(
        plant: PlacedPlant,
        gardenStyle: String,
    ): Result<String> {
        return try {
            val prompt = """
                A refined botanical portrait and sunlit close-up photograph of ${plant.commonName} (${plant.botanicalName}), a ${plant.category.lowercase()} thriving in a $gardenStyle garden during ${plant.bloomSeason} bloom. Soft natural morning light, rich organic foliage texture, warm editorial garden photography aesthetic, no text or labels.
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                imageModel.generateContent(prompt)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
                ?: return Result.failure(IllegalStateException("No plant image was returned by the model."))

            val filePath = saveBitmapToPrivateStorage(bitmap, "plant_${plant.id}_${System.currentTimeMillis()}.png")
            Result.success(filePath)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Plant visual generation timed out", e)
            Result.failure(IllegalStateException("Plant visual generation timed out after 60 seconds."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            Result.failure(IllegalStateException("Image generation quota exceeded or Blaze billing plan required."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Plant visual generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateGardenConceptVisual(
        design: GardenDesign,
        referenceYardBitmap: Bitmap? = null,
    ): Result<String> {
        return try {
            val topPlants = design.plants.take(6).joinToString(", ") { "${it.commonName} (${it.category})" }
            val hardscapes = design.hardscapeZones.joinToString(", ") { "${it.label} (${it.materialNote})" }
            val basePrompt = """
                Photorealistic architectural landscape render of a ${design.style} dream garden named "${design.title}", approximately ${design.widthFeet.toInt()} by ${design.lengthFeet.toInt()} feet, bathed in ${design.sunlight.lowercase()} light. Features hardscape elements: $hardscapes. Lushly planted with $topPlants. Warm golden-hour atmosphere, cohesive editorial landscape photography, inviting pathways and layered foliage, no text or watermarks.
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                if (referenceYardBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(referenceYardBitmap)
                            text("Transform this existing yard space into the following dream garden design while respecting its architectural boundaries: $basePrompt")
                        }
                    )
                } else {
                    imageModel.generateContent(basePrompt)
                }
            }

            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
                ?: return Result.failure(IllegalStateException("No garden concept image was returned by the model."))

            val safeId = design.id.ifBlank { UUID.randomUUID().toString().take(8) }
            val filePath = saveBitmapToPrivateStorage(bitmap, "garden_${safeId}_${System.currentTimeMillis()}.png")
            Result.success(filePath)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Garden concept render timed out", e)
            Result.failure(IllegalStateException("Garden render timed out after 60 seconds."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Garden render quota exceeded", e)
            Result.failure(IllegalStateException("Image generation quota exceeded or Blaze billing plan required."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Garden concept render failed", e)
            Result.failure(e)
        }
    }

    private suspend fun saveBitmapToPrivateStorage(bitmap: Bitmap, fileName: String): String =
        withContext(Dispatchers.IO) {
            val dir = File(appContext.filesDir, "garden_visuals").apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, stream)
            }
            outFile.absolutePath
        }

    fun loadBitmapFromPath(path: String): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun buildLayoutPrompt(prefs: GardenPreferences, hasPhoto: Boolean): String {
        val features = buildList {
            if (prefs.includePathway) add("stepping-stone or gravel pathway")
            if (prefs.includeSeating) add("cozy patio/seating zone")
            if (prefs.includeWaterFeature) add("tranquil water basin or fountain")
            if (prefs.includeEdibles) add("raised culinary herb/edible bed")
        }.joinToString(", ").ifBlank { "organic planting beds" }

        val photoInstruction = if (hasPhoto) {
            "Use the attached yard photo to infer existing light orientation, framing, and proportions."
        } else {
            ""
        }

        return """
            Design a thoughtful, botanically accurate ${prefs.style} garden layout for a ${prefs.yardType} measuring ${prefs.widthFeet} ft wide by ${prefs.lengthFeet} ft long.
            Site conditions: Sunlight = ${prefs.sunlight}; Soil = ${prefs.soilType}; Climate = ${prefs.climateZone}.
            Requested features: $features.
            ${if (prefs.titleHint.isNotBlank()) "Title inspiration: ${prefs.titleHint}." else ""}
            ${if (prefs.notes.isNotBlank()) "User notes: ${prefs.notes}." else ""}
            $photoInstruction
            Arrange 2 to 5 non-overlapping hardscape zones (using normalized 0..1 coordinates where x+width <= 0.96 and y+height <= 0.96) and 7 to 11 well-spaced plants suited to the exact sunlight, soil, and climate zone. Avoid placing plants directly on top of patio or water zones unless they are aquatic/border accents.
        """.trimIndent()
    }

    private fun parseGardenDesignJson(rawJson: String, prefs: GardenPreferences): GardenDesign {
        val root = JSONObject(rawJson)
        val title = root.optString("title", prefs.titleHint.ifBlank { "${prefs.style} Sanctuary" }).take(120)
        val summary = root.optString("summary", "").take(1000)

        val zonesArray = root.optJSONArray("hardscapeZones") ?: JSONArray()
        val zones = (0 until zonesArray.length()).map { i ->
            val z = zonesArray.getJSONObject(i)
            val x = z.optDouble("x", 0.1).coerceIn(0.02, 0.85)
            val y = z.optDouble("y", 0.1).coerceIn(0.02, 0.85)
            val w = z.optDouble("width", 0.25).coerceIn(0.08, 0.95 - x)
            val h = z.optDouble("height", 0.2).coerceIn(0.08, 0.95 - y)
            HardscapeZone(
                id = z.optString("id", "zone_${i + 1}").ifBlank { "zone_${i + 1}" },
                type = z.optString("type", "pathway"),
                label = z.optString("label", "Garden Zone"),
                x = x,
                y = y,
                width = w,
                height = h,
                materialNote = z.optString("materialNote", ""),
            )
        }

        val plantsArray = root.optJSONArray("plants") ?: JSONArray()
        val plants = (0 until plantsArray.length()).map { i ->
            val p = plantsArray.getJSONObject(i)
            PlacedPlant(
                id = p.optString("id", "plant_${i + 1}").ifBlank { "plant_${i + 1}" },
                commonName = p.optString("commonName", "Botanical Specimen"),
                botanicalName = p.optString("botanicalName", ""),
                category = p.optString("category", "Perennial"),
                sunNeed = p.optString("sunNeed", prefs.sunlight),
                waterNeed = p.optString("waterNeed", "Moderate"),
                matureSpreadFeet = p.optDouble("matureSpreadFeet", 3.0).coerceIn(1.0, 18.0),
                matureHeightFeet = p.optDouble("matureHeightFeet", 2.5).coerceIn(0.5, 25.0),
                bloomSeason = p.optString("bloomSeason", "Summer"),
                companionNote = p.optString("companionNote", ""),
                x = p.optDouble("x", 0.5).coerceIn(0.08, 0.92),
                y = p.optDouble("y", 0.5).coerceIn(0.08, 0.92),
                colorHex = sanitizeHex(p.optString("colorHex", "#4E7A5B")),
                localVisualPath = "",
            )
        }

        return GardenDesign(
            title = title,
            summary = summary,
            style = prefs.style,
            widthFeet = prefs.widthFeet,
            lengthFeet = prefs.lengthFeet,
            sunlight = prefs.sunlight,
            soilType = prefs.soilType,
            climateZone = prefs.climateZone,
            hardscapeZones = zones,
            plants = plants,
        )
    }

    private fun sanitizeHex(raw: String): String {
        val cleaned = raw.trim()
        return if (cleaned.matches(Regex("^#[0-9A-Fa-f]{6}$"))) cleaned else "#4E7A5B"
    }

    private companion object {
        const val TAG = "GardenAiRepo"
        const val AI_TIMEOUT_MS = 60_000L
    }
}
