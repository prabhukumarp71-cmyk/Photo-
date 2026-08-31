package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QualityMetrics
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VibrantEmerald
import com.example.ui.theme.WarmAmber
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun QualityMetricsCard(
    beforeMetrics: QualityMetrics?,
    afterMetrics: QualityMetrics?,
    modifier: Modifier = Modifier
) {
    if (beforeMetrics == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quality_metrics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analysis",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Optical Quality & Focus Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (beforeMetrics.resolution.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (afterMetrics != null) "4K Ultra HD" else "Input: ${beforeMetrics.resolution}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Diagnostic Summary Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnosis",
                            tint = if (afterMetrics != null) VibrantEmerald else WarmAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (afterMetrics != null) {
                                "Focus restored • Natural colors preserved • Highlights protected"
                            } else {
                                beforeMetrics.diagnosticSummary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exposure Status: ${afterMetrics?.exposureBalance ?: beforeMetrics.exposureBalance}",
                        fontSize = 11.sp,
                        color = if ((afterMetrics?.exposureBalance ?: beforeMetrics.exposureBalance).contains("Warning")) WarmAmber else VibrantEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Gauges Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus & Sharpness Index
                val sharpBefore = beforeMetrics.sharpnessScore
                val sharpAfter = afterMetrics?.sharpnessScore ?: sharpBefore
                val sharpDiffPercent = if (sharpBefore > 0) {
                    (((sharpAfter - sharpBefore) / sharpBefore) * 100).roundToInt()
                } else 0

                MetricGaugeItem(
                    title = "Focus Sharpness",
                    score = sharpAfter,
                    maxScore = 100f,
                    icon = Icons.Default.CenterFocusStrong,
                    color = ElectricCyan,
                    badgeText = if (afterMetrics != null && sharpDiffPercent > 0) "+$sharpDiffPercent%" else null,
                    modifier = Modifier.weight(1f)
                )

                // Noise Suppression Index
                val noiseBefore = beforeMetrics.noiseScore
                val noiseAfter = afterMetrics?.noiseScore ?: noiseBefore
                val noiseReductionPercent = if (noiseBefore > 0) {
                    (((noiseBefore - noiseAfter) / noiseBefore) * 100).roundToInt()
                } else 0

                MetricGaugeItem(
                    title = "Grain / Noise",
                    score = noiseAfter,
                    maxScore = 100f,
                    icon = Icons.Default.GraphicEq,
                    color = if (noiseAfter < 20f) VibrantEmerald else WarmAmber,
                    badgeText = if (afterMetrics != null && noiseReductionPercent > 0) "-$noiseReductionPercent%" else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricGaugeItem(
    title: String,
    score: Float,
    maxScore: Float,
    icon: ImageVector,
    color: Color,
    badgeText: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VibrantEmerald.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${score.roundToInt()} / 100",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (score / maxScore).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}
