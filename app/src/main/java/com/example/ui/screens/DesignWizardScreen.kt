package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.GardenStudioViewModel
import com.example.ui.viewmodel.LayoutGenUiState
import kotlin.math.roundToInt

private val GARDEN_STYLES = listOf(
    "Warm Cottage" to "Overflowing blooms & winding paths",
    "Japanese Zen" to "Moss, maples, raked gravel & calm",
    "Mediterranean" to "Lavender, olive, terracotta & stone",
    "Pollinator Haven" to "Native wildflowers & butterfly nectar",
    "Modern Minimal" to "Architectural grasses & clean pavers",
    "Tropical Oasis" to "Bold broadleaves & lush shade canopy",
)

private val YARD_TYPES = listOf("Backyard", "Courtyard", "Balcony", "Front Yard")
private val SUNLIGHT_OPTIONS = listOf("Full Sun", "Part Shade", "Full Shade")
private val SOIL_OPTIONS = listOf("Loam", "Clay", "Sandy", "Raised Bed Mix")
private val CLIMATE_OPTIONS = listOf(
    "Zone 4-5 (Cold)",
    "Zone 6-7 (Cool Temperate)",
    "Zone 7-8 (Temperate)",
    "Zone 9-10 (Warm/Coastal)",
    "Zone 11+ (Tropical)",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesignWizardScreen(
    viewModel: GardenStudioViewModel,
    onNavigateBack: () -> Unit,
    onGardenCreated: (String) -> Unit,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val referencePhoto by viewModel.referencePhoto.collectAsStateWithLifecycle()
    val genState by viewModel.layoutGenState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeDownscaledBitmap(context, uri, maxDimension = 1024)
            viewModel.setReferencePhoto(decoded)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.wizard_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.wizard_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Optional title
                OutlinedTextField(
                    value = prefs.titleHint,
                    onValueChange = { value -> viewModel.updatePreferences { it.copy(titleHint = value) } },
                    label = { Text(stringResource(R.string.wizard_name_label)) },
                    placeholder = { Text(stringResource(R.string.wizard_name_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("garden_name_input"),
                )

                // Style selector
                SectionHeader(stringResource(R.string.wizard_style_section))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    GARDEN_STYLES.forEach { (styleName, tagline) ->
                        val selected = prefs.style == styleName
                        Surface(
                            color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { viewModel.updatePreferences { it.copy(style = styleName) } }
                                .minimumInteractiveComponentSize(),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = styleName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                )
                                Text(
                                    text = tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Plot dimensions & type
                SectionHeader(stringResource(R.string.wizard_space_section))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    YARD_TYPES.forEach { yard ->
                        FilterChip(
                            selected = prefs.yardType == yard,
                            onClick = { viewModel.updatePreferences { it.copy(yardType = yard) } },
                            label = { Text(yard) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }

                Surface(
                    color = colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Width: ${prefs.widthFeet.roundToInt()} ft",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${(prefs.widthFeet * prefs.lengthFeet).roundToInt()} sq ft total",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                            )
                        }
                        Slider(
                            value = prefs.widthFeet.toFloat(),
                            onValueChange = { v ->
                                viewModel.updatePreferences { it.copy(widthFeet = v.roundToInt().toDouble()) }
                            },
                            valueRange = 6f..60f,
                        )
                        Text(
                            text = "Length: ${prefs.lengthFeet.roundToInt()} ft",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Slider(
                            value = prefs.lengthFeet.toFloat(),
                            onValueChange = { v ->
                                viewModel.updatePreferences { it.copy(lengthFeet = v.roundToInt().toDouble()) }
                            },
                            valueRange = 6f..60f,
                        )
                    }
                }

                // Sunlight, soil & climate
                SectionHeader(stringResource(R.string.wizard_conditions_section))
                Text("Sunlight", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUNLIGHT_OPTIONS.forEach { sun ->
                        FilterChip(
                            selected = prefs.sunlight == sun,
                            onClick = { viewModel.updatePreferences { it.copy(sunlight = sun) } },
                            label = { Text(sun) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }

                Text("Soil Composition", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SOIL_OPTIONS.forEach { soil ->
                        FilterChip(
                            selected = prefs.soilType == soil,
                            onClick = { viewModel.updatePreferences { it.copy(soilType = soil) } },
                            label = { Text(soil) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }

                Text("Climate Zone", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CLIMATE_OPTIONS.forEach { zone ->
                        FilterChip(
                            selected = prefs.climateZone == zone,
                            onClick = { viewModel.updatePreferences { it.copy(climateZone = zone) } },
                            label = { Text(zone) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }

                // Features toggles
                SectionHeader(stringResource(R.string.wizard_features_section))
                Surface(
                    color = colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FeatureToggleRow(
                            icon = Icons.Rounded.Terrain,
                            label = "Stepping-Stone Pathway",
                            checked = prefs.includePathway,
                            onCheckedChange = { c -> viewModel.updatePreferences { it.copy(includePathway = c) } },
                        )
                        FeatureToggleRow(
                            icon = Icons.Rounded.Weekend,
                            label = "Patio & Seating Nook",
                            checked = prefs.includeSeating,
                            onCheckedChange = { c -> viewModel.updatePreferences { it.copy(includeSeating = c) } },
                        )
                        FeatureToggleRow(
                            icon = Icons.Rounded.WaterDrop,
                            label = "Tranquil Water Feature",
                            checked = prefs.includeWaterFeature,
                            onCheckedChange = { c -> viewModel.updatePreferences { it.copy(includeWaterFeature = c) } },
                        )
                        FeatureToggleRow(
                            icon = Icons.Rounded.Restaurant,
                            label = "Culinary Herbs & Edibles",
                            checked = prefs.includeEdibles,
                            onCheckedChange = { c -> viewModel.updatePreferences { it.copy(includeEdibles = c) } },
                        )
                    }
                }

                // Optional Yard Photo
                SectionHeader(stringResource(R.string.wizard_photo_section))
                if (referencePhoto != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    ) {
                        Image(
                            bitmap = referencePhoto!!.asImageBitmap(),
                            contentDescription = "Selected reference yard photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        IconButton(
                            onClick = { viewModel.setReferencePhoto(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(colorScheme.surface.copy(alpha = 0.85f))
                                .minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.wizard_photo_clear),
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("attach_yard_photo_button"),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wizard_photo_add))
                    }
                }

                // Extra wishes
                OutlinedTextField(
                    value = prefs.notes,
                    onValueChange = { value -> viewModel.updatePreferences { it.copy(notes = value) } },
                    label = { Text(stringResource(R.string.wizard_notes_label)) },
                    placeholder = { Text(stringResource(R.string.wizard_notes_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Error feedback
                if (genState is LayoutGenUiState.Error) {
                    Surface(
                        color = colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = (genState as LayoutGenUiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                // Generate CTA
                val isGenerating = genState is LayoutGenUiState.Loading
                Button(
                    onClick = { viewModel.generateAndSaveGarden(onGardenCreated) },
                    enabled = !isGenerating,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("generate_layout_button"),
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.wizard_generating), maxLines = 1)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wizard_generate_cta))
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun FeatureToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun decodeDownscaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    } catch (e: Exception) {
        null
    }
}
