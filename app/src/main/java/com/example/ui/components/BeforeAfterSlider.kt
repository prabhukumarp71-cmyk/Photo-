package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ComparisonViewMode {
    SPLIT_SLIDER,
    SIDE_BY_SIDE,
    HOLD_ORIGINAL
}

@Composable
fun BeforeAfterViewer(
    originalBitmap: Bitmap?,
    restoredBitmap: Bitmap?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    var viewMode by remember { mutableStateOf(ComparisonViewMode.SPLIT_SLIDER) }
    var isHoldingOriginal by remember { mutableStateOf(false) }

    // Zoom & Pan transformation state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("before_after_viewer_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // View Mode Controls Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (viewMode == ComparisonViewMode.SPLIT_SLIDER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.testTag("split_mode_btn")
                    ) {
                        IconButton(
                            onClick = { viewMode = ComparisonViewMode.SPLIT_SLIDER },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Split Slider Mode",
                                tint = if (viewMode == ComparisonViewMode.SPLIT_SLIDER) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (viewMode == ComparisonViewMode.SIDE_BY_SIDE) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.testTag("side_by_side_btn")
                    ) {
                        IconButton(
                            onClick = { viewMode = ComparisonViewMode.SIDE_BY_SIDE },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Side by Side Mode",
                                tint = if (viewMode == ComparisonViewMode.SIDE_BY_SIDE) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (scale > 1.05f) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("reset_zoom_btn")
                        ) {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Reset Zoom",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (restoredBitmap != null) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (restoredBitmap != null) "✨ Restored Quality" else "📷 Original Blurry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (restoredBitmap != null) Color(0xFF34D399) else Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Image Display Area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF070B13))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat()
                val boxHeightPx = constraints.maxHeight.toFloat()

                if (originalBitmap == null) {
                    // Empty placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Photo Loaded",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else if (restoredBitmap == null || isHoldingOriginal) {
                    // Only original bitmap
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = "Original Blurry Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                    // Pill label
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "BEFORE (BLURRY)",
                            color = Color(0xFFF87171),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    // Restored image is ready!
                    when (viewMode) {
                        ComparisonViewMode.SIDE_BY_SIDE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                ) {
                                    Image(
                                        bitmap = originalBitmap.asImageBitmap(),
                                        contentDescription = "Original",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "BEFORE",
                                            color = Color(0xFFF87171),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxSize()
                                        .background(ElectricCyan)
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                ) {
                                    Image(
                                        bitmap = restoredBitmap.asImageBitmap(),
                                        contentDescription = "Restored",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "AFTER",
                                            color = ElectricCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        ComparisonViewMode.SPLIT_SLIDER,
                        ComparisonViewMode.HOLD_ORIGINAL -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    }
                                    .pointerInput(boxWidthPx) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val newFraction = splitFraction + (dragAmount.x / boxWidthPx)
                                            splitFraction = newFraction.coerceIn(0.02f, 0.98f)
                                        }
                                    }
                            ) {
                                // Full Restored Photo (Right/Base layer)
                                Image(
                                    bitmap = restoredBitmap.asImageBitmap(),
                                    contentDescription = "Restored Sharp Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Clipped Original Photo (Left layer)
                                val splitX = boxWidthPx * splitFraction
                                Image(
                                    bitmap = originalBitmap.asImageBitmap(),
                                    contentDescription = "Original Blurry Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            clipPath(
                                                Path().apply {
                                                    addRect(Rect(0f, 0f, splitX, size.height))
                                                }
                                            ) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                )

                                // Vertical Divider Line & Draggable Handle
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(splitX, 0f),
                                        end = Offset(splitX, size.height),
                                        strokeWidth = 2.5.dp.toPx()
                                    )
                                }

                                // Interactive Split Knob Handle
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .offset {
                                            IntOffset(
                                                x = (splitX - 18.dp.toPx()).roundToInt(),
                                                y = (boxHeightPx / 2 - 18.dp.toPx()).roundToInt()
                                            )
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Drag to compare",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Before / After Badges
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "ORIGINAL",
                                        color = Color(0xFFF87171),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "RESTORED",
                                        color = ElectricCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Touch Peek / Zoom Tip
            if (restoredBitmap != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 Drag slider • Pinch to zoom pixels",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isHoldingOriginal) Color(0xFFDC2626) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .testTag("hold_to_compare_btn")
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHoldingOriginal = true
                                        tryAwaitRelease()
                                        isHoldingOriginal = false
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = if (isHoldingOriginal) "Showing Original" else "Hold to Compare",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isHoldingOriginal) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
