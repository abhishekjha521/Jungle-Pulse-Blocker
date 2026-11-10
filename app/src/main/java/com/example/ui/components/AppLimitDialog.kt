package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppLimitDialog(
    appName: String,
    currentLimitMinutes: Int,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(if (currentLimitMinutes > 0) currentLimitMinutes else 30) }
    val presetOptions = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "$appName का लिमिट सेट करें" else "Set Daily Limit: $appName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isHindi)
                        "जब आप इस लिमिट तक पहुंचेंगे, तो आपको स्क्रीन टाइम ब्रेक अलर्ट मिलेगा।"
                    else
                        "When you reach this daily limit, FocusShield will alert you to pause and protect your screen time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isHindi) "चुना गया लिमिट: $selectedMinutes मिनट" else "Selected Limit: $selectedMinutes min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.forEach { mins ->
                        val label = if (mins >= 60) "${mins / 60}h ${if (mins % 60 > 0) "${mins % 60}m" else ""}" else "${mins}m"
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { selectedMinutes = mins },
                            label = { Text(label) },
                            modifier = Modifier.testTag("limit_chip_$mins")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMinutes) },
                modifier = Modifier.testTag("dialog_save_limit_button")
            ) {
                Text(if (isHindi) "सेव करें" else "Save Limit")
            }
        },
        dismissButton = {
            Row {
                if (currentLimitMinutes > 0) {
                    TextButton(
                        onClick = { onConfirm(0) },
                        modifier = Modifier.testTag("dialog_remove_limit_button")
                    ) {
                        Text(
                            text = if (isHindi) "हटाएं" else "Remove",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
