package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GardenDesign
import com.example.data.model.HardscapeZone
import com.example.data.model.PlacedPlant
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun InteractiveGardenCanvas(
    design: GardenDesign,
    selectedPlantId: String?,
    onSelectPlant: (PlacedPlant?) -> Unit,
    onDragPlant: (plantId: String, newX: Double, newY: Double) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    var canvasPixelSize by remember { mutableStateOf(IntSize(1, 1)) }
    var draggingPlantId by remember { mutableStateOf<String?>(null) }

    val currentPlants by rememberUpdatedState(design.plants)
    val currentWidthFeet by rememberUpdatedState(design.widthFeet.coerceAtLeast(4.0))
    val currentLengthFeet by rememberUpdatedState(design.lengthFeet.coerceAtLeast(4.0))

    val pulseScale by animateFloatAsState(
        targetValue = if (selectedPlantId != null) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "selectionPulse",
    )

    val plotAspectRatio = (design.widthFeet / design.lengthFeet)
        .toFloat()
        .coerceIn(0.65f, 1.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(plotAspectRatio)
            .clip(MaterialTheme.shapes.large)
            .background(colorScheme.surfaceContainerLow)
            .border(1.5.dp, colorScheme.outlineVariant, MaterialTheme.shapes.large),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasPixelSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val w = canvasPixelSize.width.toFloat().coerceAtLeast(1f)
                        val h = canvasPixelSize.height.toFloat().coerceAtLeast(1f)
                        val hit = findHitPlant(
                            tapOffset = tapOffset,
                            plants = currentPlants,
                            canvasWidthPx = w,
                            canvasHeightPx = h,
                            plotWidthFeet = currentWidthFeet,
                        )
                        onSelectPlant(hit)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val w = canvasPixelSize.width.toFloat().coerceAtLeast(1f)
                            val h = canvasPixelSize.height.toFloat().coerceAtLeast(1f)
                            val hit = findHitPlant(
                                tapOffset = startOffset,
                                plants = currentPlants,
                                canvasWidthPx = w,
                                canvasHeightPx = h,
                                plotWidthFeet = currentWidthFeet,
                            )
                            draggingPlantId = hit?.id
                            if (hit != null) {
                                onSelectPlant(hit)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val activeId = draggingPlantId ?: return@detectDragGestures
                            change.consume()
                            val w = canvasPixelSize.width.toFloat().coerceAtLeast(1f)
                            val h = canvasPixelSize.height.toFloat().coerceAtLeast(1f)
                            val plant = currentPlants.firstOrNull { it.id == activeId } ?: return@detectDragGestures
                            val nextX = plant.x + (dragAmount.x / w)
                            val nextY = plant.y + (dragAmount.y / h)
                            onDragPlant(activeId, nextX, nextY)
                        },
                        onDragEnd = {
                            if (draggingPlantId != null) {
                                draggingPlantId = null
                                onDragFinished()
                            }
                        },
                        onDragCancel = {
                            if (draggingPlantId != null) {
                                draggingPlantId = null
                                onDragFinished()
                            }
                        },
                    )
                },
        ) {
            val w = size.width
            val h = size.height

            // 1. Subtle botanical drafting paper grid
            val gridCols = 8
            val gridRows = 8
            val gridColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
            for (c in 1 until gridCols) {
                val xPos = w * (c.toFloat() / gridCols)
                drawLine(
                    color = gridColor,
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, h),
                    strokeWidth = 0.8.dp.toPx(),
                )
            }
            for (r in 1 until gridRows) {
                val yPos = h * (r.toFloat() / gridRows)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, yPos),
                    end = Offset(w, yPos),
                    strokeWidth = 0.8.dp.toPx(),
                )
            }

            // 2. Hardscape zones
            design.hardscapeZones.forEach { zone ->
                val zoneColor = hardscapeColor(zone.type)
                val topLeft = Offset((zone.x * w).toFloat(), (zone.y * h).toFloat())
                val rectSize = Size((zone.width * w).toFloat(), (zone.height * h).toFloat())
                val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())

                drawRoundRect(
                    color = zoneColor.copy(alpha = 0.35f),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = zoneColor.copy(alpha = 0.85f),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = corner,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = if (zone.type == "pathway") {
                            PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        } else null,
                    ),
                )

                if (rectSize.width > 48.dp.toPx() && rectSize.height > 20.dp.toPx()) {
                    val labelLayout = textMeasurer.measure(
                        text = zone.label.take(18),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface.copy(alpha = 0.72f),
                        ),
                    )
                    val labelX = topLeft.x + (rectSize.width - labelLayout.size.width) / 2f
                    val labelY = topLeft.y + (rectSize.height - labelLayout.size.height) / 2f
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(
                            labelX.coerceIn(4f, max(4f, w - labelLayout.size.width - 4f)),
                            labelY.coerceIn(4f, max(4f, h - labelLayout.size.height - 4f)),
                        ),
                    )
                }
            }

            // 3. Placed plant canopies
            design.plants.forEach { plant ->
                val isSelected = plant.id == selectedPlantId
                val center = Offset((plant.x * w).toFloat(), (plant.y * h).toFloat())
                val baseRadius = plantRadiusPx(
                    matureSpreadFeet = plant.matureSpreadFeet,
                    plotWidthFeet = design.widthFeet,
                    canvasWidthPx = w,
                )
                val canopyColor = parsePlantColor(plant.colorHex)

                if (isSelected) {
                    drawCircle(
                        color = colorScheme.secondary.copy(alpha = 0.22f),
                        radius = baseRadius * pulseScale + 6.dp.toPx(),
                        center = center,
                    )
                    drawCircle(
                        color = colorScheme.secondary,
                        radius = baseRadius * pulseScale + 3.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }

                // Outer translucent foliage halo
                drawCircle(
                    color = canopyColor.copy(alpha = 0.32f),
                    radius = baseRadius,
                    center = center,
                )
                // Inner dense crown
                drawCircle(
                    color = canopyColor.copy(alpha = 0.85f),
                    radius = baseRadius * 0.68f,
                    center = center,
                )
                // Crisp botanical outline
                drawCircle(
                    color = canopyColor,
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 1.4.dp.toPx()),
                )

                val shortLabel = plant.commonName.split(" ").firstOrNull()?.take(9).orEmpty()
                if (shortLabel.isNotBlank()) {
                    val labelLayout = textMeasurer.measure(
                        text = shortLabel,
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    val pillW = labelLayout.size.width + 10.dp.toPx()
                    val pillH = labelLayout.size.height + 4.dp.toPx()
                    val pillLeft = (center.x - pillW / 2f).coerceIn(4f, max(4f, w - pillW - 4f))
                    val pillTop = (center.y + baseRadius * 0.35f).coerceIn(4f, max(4f, h - pillH - 4f))
                    drawRoundRect(
                        color = colorScheme.onSurface.copy(alpha = 0.72f),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(pillLeft + 5.dp.toPx(), pillTop + 2.dp.toPx()),
                    )
                }
            }
        }

        // Dimensions badge in corner
        Surface(
            color = colorScheme.surface.copy(alpha = 0.88f),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
        ) {
            Text(
                text = "${design.widthFeet.toInt()} × ${design.lengthFeet.toInt()} ft",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun MiniGardenBlueprintThumbnail(
    design: GardenDesign,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Canvas(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        val w = size.width
        val h = size.height

        // Mini grid lines
        for (i in 1..3) {
            drawLine(
                color = colorScheme.outlineVariant.copy(alpha = 0.45f),
                start = Offset(w * (i / 4f), 0f),
                end = Offset(w * (i / 4f), h),
                strokeWidth = 0.7.dp.toPx(),
            )
            drawLine(
                color = colorScheme.outlineVariant.copy(alpha = 0.45f),
                start = Offset(0f, h * (i / 4f)),
                end = Offset(w, h * (i / 4f)),
                strokeWidth = 0.7.dp.toPx(),
            )
        }

        design.hardscapeZones.forEach { zone ->
            drawRoundRect(
                color = hardscapeColor(zone.type).copy(alpha = 0.4f),
                topLeft = Offset((zone.x * w).toFloat(), (zone.y * h).toFloat()),
                size = Size((zone.width * w).toFloat(), (zone.height * h).toFloat()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )
        }

        design.plants.forEach { plant ->
            val r = min(w, h) * 0.08f
            val c = Offset((plant.x * w).toFloat(), (plant.y * h).toFloat())
            val plantColor = parsePlantColor(plant.colorHex)
            drawCircle(color = plantColor.copy(alpha = 0.35f), radius = r * 1.35f, center = c)
            drawCircle(color = plantColor.copy(alpha = 0.9f), radius = r, center = c)
        }
    }
}

private fun findHitPlant(
    tapOffset: Offset,
    plants: List<PlacedPlant>,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    plotWidthFeet: Double,
): PlacedPlant? {
    return plants.asReversed().firstOrNull { plant ->
        val cx = (plant.x * canvasWidthPx).toFloat()
        val cy = (plant.y * canvasHeightPx).toFloat()
        val hitRadius = max(
            plantRadiusPx(plant.matureSpreadFeet, plotWidthFeet, canvasWidthPx) * 1.25f,
            26f,
        )
        hypot(tapOffset.x - cx, tapOffset.y - cy) <= hitRadius
    }
}

private fun plantRadiusPx(
    matureSpreadFeet: Double,
    plotWidthFeet: Double,
    canvasWidthPx: Float,
): Float {
    val safePlotWidth = plotWidthFeet.coerceAtLeast(6.0)
    val proportional = ((matureSpreadFeet / 2.0) / safePlotWidth).toFloat() * canvasWidthPx
    return proportional.coerceIn(canvasWidthPx * 0.042f, canvasWidthPx * 0.135f)
}

private fun hardscapeColor(type: String): Color = when (type.lowercase()) {
    "water" -> Color(0xFF4F8CA8)
    "patio", "deck" -> Color(0xFFB87A56)
    "pathway" -> Color(0xFFA9A292)
    "bed" -> Color(0xFF7B634E)
    "lawn" -> Color(0xFF83A86C)
    else -> Color(0xFF8D948A)
}

fun parsePlantColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val value = cleaned.toLong(16)
        Color(0xFF000000L or value)
    } catch (e: Exception) {
        Color(0xFF4E7A5B)
    }
}
