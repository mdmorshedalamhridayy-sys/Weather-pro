package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun SatelliteMap(
    modifier: Modifier = Modifier,
    showRadar: Boolean = true,
    showClouds: Boolean = true,
    showCycloneCone: Boolean = true
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Live meteorological infrared satellite imagery of the Bay of Bengal
    val satelliteImageUrl = "https://img.weather.com/images/sat/sasiasat_600x410.jpg"

    // Pulsing animations for active storm center
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 5.0f)
                    val maxOffset = 500f * scale
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                    )
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            // 1. Satellite Base layer (Coil loads visual satellite background)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(satelliteImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Bay of Bengal Satellite View",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 2. Custom Meteorological Canvas Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw latitude/longitude grid (isobars)
                val gridColor = Color(0x3338BDF8)
                val gridStroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f)))
                
                // Horizontal latitude lines
                for (i in 1..8) {
                    val y = height * (i / 9f)
                    drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1.6f, pathEffect = gridStroke.pathEffect)
                }
                // Vertical longitude lines
                for (i in 1..8) {
                    val x = width * (i / 9f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1.6f, pathEffect = gridStroke.pathEffect)
                }

                // Coastlines path for Bangladesh Delta / Ganges
                val coastPath = Path().apply {
                    moveTo(width * 0.15f, height * 0.35f)
                    quadraticTo(width * 0.25f, height * 0.38f, width * 0.35f, height * 0.35f)
                    lineTo(width * 0.42f, height * 0.33f)
                    quadraticTo(width * 0.44f, height * 0.22f, width * 0.45f, height * 0.34f)
                    lineTo(width * 0.50f, height * 0.44f)
                    quadraticTo(width * 0.55f, height * 0.55f, width * 0.52f, height * 0.70f)
                    lineTo(width * 0.58f, height * 0.85f)
                }

                drawPath(
                    path = coastPath,
                    color = Color(0x7FFFFD66),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Label Bangladesh Coast using drawIntoCanvas
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.YELLOW
                        textSize = 28f
                        isFakeBoldText = true
                        alpha = 180
                    }
                    nativeCanvas.drawText("BANGLADESH (GANGES DELTA)", width * 0.22f, height * 0.28f, paint)
                    nativeCanvas.drawText("BAY OF BENGAL", width * 0.30f, height * 0.55f, paint)
                    nativeCanvas.drawText("COX'S BAZAR", width * 0.55f, height * 0.44f, paint)
                }

                // 3. Storm Radar clouds & cyclones drawing
                if (showRadar) {
                    val stormCenter = Offset(width * 0.40f, height * 0.52f)

                    drawCircle(
                        color = Color(0x22EF4444),
                        radius = pulseSize * 3,
                        center = stormCenter
                    )
                    drawCircle(
                        color = Color(0x33F97316),
                        radius = pulseSize * 2,
                        center = stormCenter
                    )
                    drawCircle(
                        color = Color(0x99EF4444),
                        radius = 12f,
                        center = stormCenter
                    )

                    // Draw rotating hurricane cloud spirals
                    val cloudPath = Path().apply {
                        for (i in 0..3) {
                            val angle = Math.toRadians((rotationAngle + i * 90).toDouble())
                            val endX = stormCenter.x + Math.cos(angle).toFloat() * 120f
                            val endY = stormCenter.y + Math.sin(angle).toFloat() * 120f
                            moveTo(stormCenter.x, stormCenter.y)
                            quadraticTo(
                                (stormCenter.x + endX) / 2f + 40f,
                                (stormCenter.y + endY) / 2f - 40f,
                                endX,
                                endY
                            )
                        }
                    }
                    drawPath(
                        path = cloudPath,
                        color = Color(0x55F87171),
                        style = Stroke(width = 12f)
                    )

                    // Label active hazard status using drawIntoCanvas
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val alertPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.RED
                            textSize = 34f
                            isFakeBoldText = true
                        }
                        nativeCanvas.drawText("CYCLONE REMAL core [980 hPa]", stormCenter.x - 170f, stormCenter.y - 70f, alertPaint)
                    }
                }

                // 4. Cyclone Core path projections
                if (showCycloneCone) {
                    val startPt = Offset(width * 0.40f, height * 0.52f)
                    val pathCone = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        quadraticTo(
                            width * 0.43f, height * 0.42f,
                            width * 0.45f, height * 0.32f
                        )
                    }

                    val coneOutline = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        lineTo(width * 0.35f, height * 0.30f)
                        lineTo(width * 0.55f, height * 0.30f)
                        close()
                    }
                    drawPath(
                        path = coneOutline,
                        color = Color(0x15F1F5F9)
                    )

                    drawPath(
                        path = pathCone,
                        color = Color(0xFF38BDF8),
                        style = Stroke(
                            width = 5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
                        )
                    )

                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 20f,
                        center = Offset(width * 0.45f, height * 0.32f),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        // Floating Zoom Controls (HUD)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(Color(0xD91E293B), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = { scale = (scale + 0.4f).coerceAtMost(5.0f) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            FilledIconButton(
                onClick = { scale = (scale - 0.4f).coerceAtLeast(0.8f) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
            FilledIconButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.White)
            }
        }

        // Status banner in top left overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .width(220.dp)
                .background(Color(0xD90F172A), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "BAROMETRIC SATELLITE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF0284C7)
            )
            Text(
                text = "Bay of Bengal (IR Loop)",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, RoundedCornerShape(50))
                )
                Text(
                    text = "Live feeds connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}
