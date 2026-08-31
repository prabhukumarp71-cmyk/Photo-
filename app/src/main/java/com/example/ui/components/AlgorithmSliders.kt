package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ManualEnhancementParams
import com.example.ui.theme.ElectricCyan
import java.util.Locale

@Composable
fun AlgorithmStudioSliders(
    params: ManualEnhancementParams,
    onParamsChanged: (ManualEnhancementParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("algorithm_sliders_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pro Algorithmic Tuning",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = { onParamsChanged(ManualEnhancementParams()) },
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reset Defaults",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Reset", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Sharpen Amount (Controlled, Non-Ringing)
            ParameterSliderItem(
                label = "Edge Sharpening (Luminance Only)",
                value = params.sharpenAmount,
                valueRange = 0.2f..2.5f,
                formattedValue = String.format(Locale.US, "%.1fx", params.sharpenAmount),
                onValueChange = { onParamsChanged(params.copy(sharpenAmount = it)) }
            )

            // 2. Inverse Deblur (PSF focus recovery)
            ParameterSliderItem(
                label = "Intelligent Deblur (Halo-Free PSF)",
                value = params.deblurInverseStrength,
                valueRange = 0.0f..1.0f,
                formattedValue = "${(params.deblurInverseStrength * 100).toInt()}%",
                onValueChange = { onParamsChanged(params.copy(deblurInverseStrength = it)) }
            )

            // 3. Bilateral Noise Reduction
            ParameterSliderItem(
                label = "Natural Noise Reduction",
                value = params.denoiseAmount,
                valueRange = 0.0f..1.0f,
                formattedValue = "${(params.denoiseAmount * 100).toInt()}%",
                onValueChange = { onParamsChanged(params.copy(denoiseAmount = it)) }
            )

            // 4. Micro-Contrast / Clarity (Safe range to prevent overexposure)
            ParameterSliderItem(
                label = "Tone Clarity (Highlight-Protected)",
                value = params.clarityContrast,
                valueRange = 0.95f..1.25f,
                formattedValue = String.format(Locale.US, "%.2fx", params.clarityContrast),
                onValueChange = { onParamsChanged(params.copy(clarityContrast = it)) }
            )
        }
    }
}

@Composable
private fun ParameterSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedValue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
