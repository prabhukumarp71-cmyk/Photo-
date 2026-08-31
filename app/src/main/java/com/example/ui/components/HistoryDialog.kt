package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.RestorationRecord
import com.example.ui.theme.ElectricCyan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDialog(
    historyList: List<RestorationRecord>,
    onSelectRecord: (RestorationRecord) -> Unit,
    onDeleteRecord: (RestorationRecord) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(550.dp)
            .testTag("history_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = ElectricCyan
                    )
                    Text(
                        text = "Restoration History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                if (historyList.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No saved restorations yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(historyList, key = { it.id }) { record ->
                        HistoryItemCard(
                            record = record,
                            onSelect = {
                                onSelectRecord(record)
                                onDismiss()
                            },
                            onDelete = { onDeleteRecord(record) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun HistoryItemCard(
    record: RestorationRecord,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("history_item_${record.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restored Thumbnail
            Image(
                painter = rememberAsyncImagePainter(File(record.restoredImagePath)),
                contentDescription = "Restored Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$dateStr • ${record.mode}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElectricCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Sharpness: ${record.sharpnessAfter.toInt()}",
                            fontSize = 10.sp,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
