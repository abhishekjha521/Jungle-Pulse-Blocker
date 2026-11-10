package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.ColorFocusMode
import com.example.viewmodel.MainViewModel

@Composable
fun FocusModeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    val focusSession by viewModel.focusSession.collectAsState()
    var selectedPresetMinutes by remember { mutableIntStateOf(25) }
    var selectedSessionTitle by remember { mutableStateOf("Deep Work") }

    val remainingSec = focusSession.remainingSeconds
    val totalSec = focusSession.totalSeconds
    val minutes = remainingSec / 60
    val seconds = remainingSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val progress = if (totalSec > 0) {
        (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "focus_progress")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi) "फ़ोकस मोड और ऐप लॉक" else "Focus Mode & Distraction Shield",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi)
                    "काम या पढ़ाई के दौरान YouTube और सोशल ऐप्स को लॉक रखें और स्क्रीन टाइम नियंत्रित करें।"
                else
                    "Lock distracting video & social apps during study or work sessions to stay productive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Circular Timer Visualizer
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = surfaceVariant,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ColorFocusMode,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (focusSession.isActive) Icons.Default.Lock else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = ColorFocusMode,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (focusSession.isActive) {
                            if (isHindi) "सत्र चालू है" else "Session Active"
                        } else {
                            if (isHindi) "तैयार" else "Ready"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Preset Duration Chips (if not active)
        if (!focusSession.isActive) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val presets = listOf(
                        15 to "Quick Break",
                        25 to "Pomodoro",
                        45 to "Deep Work",
                        60 to "Intense"
                    )
                    presets.forEach { (mins, name) ->
                        FilterChip(
                            selected = selectedPresetMinutes == mins,
                            onClick = {
                                selectedPresetMinutes = mins
                                selectedSessionTitle = name
                            },
                            label = { Text("${mins}m") },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("focus_preset_$mins")
                        )
                    }
                }
            }
        }

        // Start / Stop Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!focusSession.isActive) {
                    Button(
                        onClick = {
                            viewModel.startFocusSession(selectedPresetMinutes, selectedSessionTitle)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("start_focus_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorFocusMode)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "फ़ोकस सेशन शुरू करें (${selectedPresetMinutes}m)" else "Start Focus Session (${selectedPresetMinutes}m)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopFocusSession() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("stop_focus_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "सेशन समाप्त करें" else "Stop Session",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Blocked Apps Card during focus
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_blocked_apps_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ColorFocusMode
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "फ़ोकस के दौरान सुरक्षित और ब्लॉक किए गए ऐप्स" else "Shielded Apps During Focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isHindi)
                            "• YouTube और वीडियो ऐप्स: नोटिफिकेशन म्यूट और समय सीमा लागू\n• Instagram और सोशल ऐप्स: ध्यान भटकाने से रोकें\n• आने वाले डिस्टर्बिंग पॉपअप्स ब्लॉक रहेंगे"
                        else
                            "• YouTube & video streaming: strict usage guard & pause prompts\n• Social Media (Instagram, WhatsApp): notifications silenced\n• Full distraction-free environment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Motivational Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_quote_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "💡 डिजिटल वेलबीइंग विचार" else "💡 Digital Wellbeing Tip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi)
                            "\"जब आप विज्ञापनों और शॉर्ट वीडियो के अंतहीन लूप से बचते हैं, तो आप हर दिन औसतन 1 से 2 घंटे बचाते हैं!\""
                        else
                            "\"By skipping ads and setting healthy limits on video apps, users save an average of 1.5 hours of screen time every day!\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
