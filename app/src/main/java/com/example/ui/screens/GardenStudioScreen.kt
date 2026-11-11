package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.GardenDesign
import com.example.data.model.PlacedPlant
import com.example.ui.components.InteractiveGardenCanvas
import com.example.ui.components.parsePlantColor
import com.example.ui.viewmodel.CompanionUiState
import com.example.ui.viewmodel.GardenStudioViewModel
import com.example.ui.viewmodel.VisualGenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenStudioScreen(
    gardenId: String,
    viewModel: GardenStudioViewModel,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(gardenId) {
        viewModel.loadGarden(gardenId)
    }

    val design by viewModel.activeGarden.collectAsStateWithLifecycle()
    val studioError by viewModel.studioError.collectAsStateWithLifecycle()
    val plantVisualState by viewModel.plantVisualState.collectAsStateWithLifecycle()
    val conceptVisualState by viewModel.conceptVisualState.collectAsStateWithLifecycle()
    val companionState by viewModel.companionState.collectAsStateWithLifecycle()

    var selectedPlantId by remember { mutableStateOf<String?>(null) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showAddCompanionDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val colorScheme = MaterialTheme.colorScheme
    val activeDesign = design
    val inspectedPlant = activeDesign?.plants?.firstOrNull { it.id == selectedPlantId }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeDesign?.title ?: "Garden Studio",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                        )
                        if (activeDesign != null) {
                            Text(
                                text = "${activeDesign.style} • ${activeDesign.sunlight} • ${activeDesign.climateZone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
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
                actions = {
                    if (activeDesign != null) {
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Rename Garden")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background),
            )
        },
    ) { innerPadding ->
        if (activeDesign == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!studioError.isNullOrBlank()) {
                Surface(
                    color = colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = studioError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.clearStudioError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 840.dp
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LayoutCanvasSection(
                                design = activeDesign,
                                selectedPlantId = selectedPlantId,
                                onSelectPlant = { plant ->
                                    selectedPlantId = plant?.id
                                    if (plant != null) {
                                        viewModel.resetPlantVisualState()
                                        showInspectorSheet = true
                                    }
                                },
                                onDragPlant = viewModel::movePlantInMemory,
                                onDragFinished = viewModel::persistActiveGarden,
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ConceptVisualSection(
                                design = activeDesign,
                                visualState = conceptVisualState,
                                loadBitmap = viewModel::loadBitmap,
                                onGenerateConcept = viewModel::generateGardenConceptVisual,
                            )
                            PlantPaletteSection(
                                design = activeDesign,
                                selectedPlantId = selectedPlantId,
                                onSelectPlant = { plant ->
                                    selectedPlantId = plant.id
                                    viewModel.resetPlantVisualState()
                                    showInspectorSheet = true
                                },
                                onAddCompanionClick = {
                                    viewModel.requestCompanionSuggestions("")
                                    showAddCompanionDialog = true
                                },
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = colorScheme.background,
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text(stringResource(R.string.studio_tab_layout)) },
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text(stringResource(R.string.studio_tab_visuals)) },
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                text = { Text(stringResource(R.string.studio_tab_plants)) },
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (selectedTabIndex) {
                                0 -> LayoutCanvasSection(
                                    design = activeDesign,
                                    selectedPlantId = selectedPlantId,
                                    onSelectPlant = { plant ->
                                        selectedPlantId = plant?.id
                                        if (plant != null) {
                                            viewModel.resetPlantVisualState()
                                            showInspectorSheet = true
                                        }
                                    },
                                    onDragPlant = viewModel::movePlantInMemory,
                                    onDragFinished = viewModel::persistActiveGarden,
                                )

                                1 -> ConceptVisualSection(
                                    design = activeDesign,
                                    visualState = conceptVisualState,
                                    loadBitmap = viewModel::loadBitmap,
                                    onGenerateConcept = viewModel::generateGardenConceptVisual,
                                )

                                else -> PlantPaletteSection(
                                    design = activeDesign,
                                    selectedPlantId = selectedPlantId,
                                    onSelectPlant = { plant ->
                                        selectedPlantId = plant.id
                                        viewModel.resetPlantVisualState()
                                        showInspectorSheet = true
                                    },
                                    onAddCompanionClick = {
                                        viewModel.requestCompanionSuggestions("")
                                        showAddCompanionDialog = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInspectorSheet && inspectedPlant != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            sheetState = sheetState,
        ) {
            PlantInspectorSheetContent(
                plant = inspectedPlant,
                visualState = plantVisualState,
                companionState = companionState,
                loadBitmap = viewModel::loadBitmap,
                onGeneratePlantVisual = { viewModel.generatePlantVisual(inspectedPlant) },
                onRequestSwapIdeas = { viewModel.requestCompanionSuggestions("Replace or complement ${inspectedPlant.commonName}") },
                onSwapWithPlant = { replacement ->
                    viewModel.addOrSwapPlant(targetPlantId = inspectedPlant.id, newPlant = replacement)
                },
                onRemovePlant = {
                    viewModel.removePlant(inspectedPlant.id)
                    selectedPlantId = null
                    showInspectorSheet = false
                },
            )
        }
    }

    if (showAddCompanionDialog) {
        AddCompanionDialog(
            companionState = companionState,
            onDismiss = { showAddCompanionDialog = false },
            onRetry = { viewModel.requestCompanionSuggestions("") },
            onSelectPlant = { plant ->
                viewModel.addOrSwapPlant(targetPlantId = null, newPlant = plant)
                showAddCompanionDialog = false
            },
        )
    }

    if (showRenameDialog && activeDesign != null) {
        var draftTitle by remember(activeDesign.title) { mutableStateOf(activeDesign.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Garden") },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                    label = { Text("Garden Title") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateGardenTitle(draftTitle)
                        showRenameDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LayoutCanvasSection(
    design: GardenDesign,
    selectedPlantId: String?,
    onSelectPlant: (PlacedPlant?) -> Unit,
    onDragPlant: (String, Double, Double) -> Unit,
    onDragFinished: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.studio_drag_hint),
        style = MaterialTheme.typography.labelMedium,
        color = colorScheme.onSurfaceVariant,
    )

    InteractiveGardenCanvas(
        design = design,
        selectedPlantId = selectedPlantId,
        onSelectPlant = onSelectPlant,
        onDragPlant = onDragPlant,
        onDragFinished = onDragFinished,
        modifier = Modifier.testTag("garden_layout_canvas"),
    )

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Landscape Summary",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = design.summary,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
            if (design.hardscapeZones.isNotEmpty()) {
                HorizontalDivider(color = colorScheme.outlineVariant)
                Text(
                    text = "Hardscape Zones",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                )
                design.hardscapeZones.forEach { zone ->
                    Text(
                        text = "• ${zone.label}: ${zone.materialNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConceptVisualSection(
    design: GardenDesign,
    visualState: VisualGenUiState,
    loadBitmap: (String) -> android.graphics.Bitmap?,
    onGenerateConcept: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bitmap = remember(design.conceptVisualPath) { loadBitmap(design.conceptVisualPath) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.large),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Dream Garden Concept Visual",
                style = MaterialTheme.typography.titleLarge,
            )

            when (visualState) {
                is VisualGenUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp),
                        ) {
                            CircularProgressIndicator(color = colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.studio_rendering_image),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = design.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(colorScheme.surfaceContainerLow)
                                .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Synthesize a full visual preview of your ${design.style} layout and companion plantings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (visualState is VisualGenUiState.Error) {
                Surface(
                    color = colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = visualState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Button(
                onClick = onGenerateConcept,
                enabled = visualState !is VisualGenUiState.Loading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("render_garden_visual_button"),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (bitmap != null) {
                        stringResource(R.string.studio_rerender_scene)
                    } else {
                        stringResource(R.string.studio_render_scene)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlantPaletteSection(
    design: GardenDesign,
    selectedPlantId: String?,
    onSelectPlant: (PlacedPlant) -> Unit,
    onAddCompanionClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Curated Plants (${design.plants.size})",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedButton(
                onClick = onAddCompanionClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_companion_plant_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI Add")
            }
        }

        design.plants.forEach { plant ->
            val isSelected = plant.id == selectedPlantId
            val plantColor = parsePlantColor(plant.colorHex)
            val thumbBitmap = remember(plant.localVisualPath) { loadBitmapOrNull(plant.localVisualPath) }

            Surface(
                color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.55f) else colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSelectPlant(plant) }
                    .minimumInteractiveComponentSize()
                    .testTag("plant_row_${plant.id}"),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (thumbBitmap != null) {
                        Image(
                            bitmap = thumbBitmap.asImageBitmap(),
                            contentDescription = plant.commonName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(plantColor.copy(alpha = 0.2f))
                                .border(2.dp, plantColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFlorist,
                                contentDescription = null,
                                tint = plantColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plant.commonName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                        )
                        if (plant.botanicalName.isNotBlank()) {
                            Text(
                                text = plant.botanicalName,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${plant.category} • ${plant.matureHeightFeet}ft H × ${plant.matureSpreadFeet}ft W • ${plant.bloomSeason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantInspectorSheetContent(
    plant: PlacedPlant,
    visualState: VisualGenUiState,
    companionState: CompanionUiState,
    loadBitmap: (String) -> android.graphics.Bitmap?,
    onGeneratePlantVisual: () -> Unit,
    onRequestSwapIdeas: () -> Unit,
    onSwapWithPlant: (PlacedPlant) -> Unit,
    onRemovePlant: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bitmap = remember(plant.localVisualPath) { loadBitmap(plant.localVisualPath) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.commonName,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (plant.botanicalName.isNotBlank()) {
                    Text(
                        text = plant.botanicalName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onRemovePlant,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove plant",
                    tint = colorScheme.error,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfoBadge(icon = Icons.Rounded.WbSunny, text = plant.sunNeed)
            InfoBadge(icon = Icons.Rounded.WaterDrop, text = "${plant.waterNeed} water")
            InfoBadge(icon = Icons.Rounded.Height, text = "${plant.matureHeightFeet}ft H × ${plant.matureSpreadFeet}ft W")
            InfoBadge(icon = Icons.Rounded.LocalFlorist, text = "${plant.bloomSeason} bloom")
        }

        if (plant.companionNote.isNotBlank()) {
            Surface(
                color = colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = plant.companionNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Plant AI Visual area
        when (visualState) {
            is VisualGenUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.studio_rendering_image),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            else -> {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = plant.commonName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    )
                }
            }
        }

        if (visualState is VisualGenUiState.Error) {
            Surface(
                color = colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = visualState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onGeneratePlantVisual,
                enabled = visualState !is VisualGenUiState.Loading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("generate_plant_visual_button"),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (bitmap != null) "Regenerate" else stringResource(R.string.studio_visualize_plant),
                    maxLines = 1,
                )
            }

            OutlinedButton(
                onClick = onRequestSwapIdeas,
                enabled = companionState !is CompanionUiState.Loading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("swap_plant_ideas_button"),
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Swap Ideas", maxLines = 1)
            }
        }

        when (companionState) {
            is CompanionUiState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Finding companion alternatives...", style = MaterialTheme.typography.bodySmall)
                }
            }

            is CompanionUiState.Error -> {
                Text(
                    text = companionState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )
            }

            is CompanionUiState.Success -> {
                Text(
                    text = "Tap an alternative to swap:",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                )
                companionState.suggestions.forEach { suggestion ->
                    Surface(
                        color = colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSwapWithPlant(suggestion) }
                            .minimumInteractiveComponentSize(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(suggestion.commonName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${suggestion.botanicalName} • ${suggestion.companionNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            CompanionUiState.Idle -> Unit
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AddCompanionDialog(
    companionState: CompanionUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelectPlant: (PlacedPlant) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.studio_add_plant)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (companionState) {
                    is CompanionUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Text("Curating botanical companions...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    is CompanionUiState.Error -> {
                        Text(companionState.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                    }

                    is CompanionUiState.Success -> {
                        companionState.suggestions.forEach { plant ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onSelectPlant(plant) }
                                    .minimumInteractiveComponentSize(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(plant.commonName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${plant.botanicalName} (${plant.category})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = plant.companionNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    CompanionUiState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun loadBitmapOrNull(path: String): android.graphics.Bitmap? {
    if (path.isBlank()) return null
    val file = java.io.File(path)
    if (!file.exists()) return null
    return android.graphics.BitmapFactory.decodeFile(file.absolutePath)
}
