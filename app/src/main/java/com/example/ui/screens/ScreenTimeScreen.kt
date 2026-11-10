package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppCategory
import com.example.model.AppUsageInfo
import com.example.ui.components.AppLimitDialog
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.ColorLimitExceeded
import com.example.ui.theme.ColorTimeWarning
import com.example.viewmodel.MainViewModel

@Composable
fun ScreenTimeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    val usageStats by viewModel.usageStats.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()

    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var appToEditLimit by remember { mutableStateOf<AppUsageInfo?>(null) }
    var isBreakAlertEnabled by remember { mutableStateOf(true) }
    var isBedtimeEnabled by remember { mutableStateOf(false) }

    val totalMinutes = usageStats.sumOf { it.usageMinutes }
    val formattedTotalTime = "${totalMinutes / 60}h ${totalMinutes % 60}m"

    val filteredList = if (selectedCategory == null) {
        usageStats
    } else {
        usageStats.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Screen Time Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screen_time_overview_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "आज का कुल स्क्रीन टाइम" else "Today's Total Screen Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedTotalTime,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshUsageStats() },
                            modifier = Modifier.testTag("refresh_usage_stats_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Stats",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category distribution badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val videoTime = usageStats.filter { it.category == AppCategory.VIDEO }.sumOf { it.usageMinutes }
                        val socialTime = usageStats.filter { it.category == AppCategory.SOCIAL }.sumOf { it.usageMinutes }

                        Text(
                            text = if (isHindi) "🎬 वीडियो: ${videoTime}m" else "🎬 Video: ${videoTime}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isHindi) "💬 सोशल: ${socialTime}m" else "💬 Social: ${socialTime}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Usage Permission Notice (if needed)
        if (!hasUsagePermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permission_banner_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorTimeWarning.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ColorTimeWarning
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "Usage Access की आवश्यकता है" else "Usage Access Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorTimeWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi)
                                "सटीक स्क्रीन टाइम और ऐप लिमिट्स देखने के लिए कृपया सेटिंग्स में FocusShield को Usage Access की अनुमति दें।"
                            else
                                "To track real-time app usage and enforce daily limits, please grant Usage Access to FocusShield in Android Settings.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.testTag("enable_usage_perm_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isHindi) "सेटिंग्स में अनुमति दें" else "Grant in Settings")
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(if (isHindi) "सभी ऐप्स" else "All Apps") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = selectedCategory == AppCategory.VIDEO,
                    onClick = { selectedCategory = AppCategory.VIDEO },
                    label = { Text(if (isHindi) "वीडियो" else "Video") },
                    modifier = Modifier.testTag("filter_video")
                )
                FilterChip(
                    selected = selectedCategory == AppCategory.SOCIAL,
                    onClick = { selectedCategory = AppCategory.SOCIAL },
                    label = { Text(if (isHindi) "सोशल" else "Social") },
                    modifier = Modifier.testTag("filter_social")
                )
            }
        }

        // App List with Daily Limits
        item {
            Text(
                text = if (isHindi) "ऐप्स और डेली लिमिट्स (Daily Limits)" else "Apps & Daily Usage Limits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(filteredList) { app ->
            val isExceeded = app.isLimitExceeded
            val hasLimit = app.dailyLimitMinutes > 0
            val progress = app.progressRatio

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_usage_card_${app.packageName}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExceeded) {
                        ColorLimitExceeded.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        when (app.category) {
                                            AppCategory.VIDEO -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            AppCategory.SOCIAL -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                            AppCategory.PRODUCTIVITY -> Color(0xFF10B981).copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = when (app.category) {
                                        AppCategory.VIDEO -> Color(0xFFEF4444)
                                        AppCategory.SOCIAL -> Color(0xFF3B82F6)
                                        AppCategory.PRODUCTIVITY -> Color(0xFF10B981)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) app.category.displayNameHi else app.category.displayNameEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = app.formattedTime,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) ColorLimitExceeded else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasLimit) {
                                Text(
                                    text = if (isHindi) "लिमिट: ${app.dailyLimitMinutes}m" else "Limit: ${app.dailyLimitMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isExceeded) ColorLimitExceeded else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (hasLimit) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                isExceeded -> ColorLimitExceeded
                                progress > 0.8f -> ColorTimeWarning
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExceeded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ColorLimitExceeded,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "दैनिक लिमिट पार हो गया!" else "Daily Limit Reached!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorLimitExceeded,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = if (hasLimit) {
                                    val remaining = (app.dailyLimitMinutes - app.usageMinutes).coerceAtLeast(0)
                                    if (isHindi) "$remaining मिनट बाकी" else "$remaining min remaining"
                                } else {
                                    if (isHindi) "कोई लिमिट सेट नहीं है" else "No limit set"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { appToEditLimit = app },
                            modifier = Modifier.testTag("set_limit_btn_${app.packageName}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasLimit) (if (isHindi) "बदलें" else "Edit") else (if (isHindi) "लिमिट लगाएं" else "Set Limit"),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // Extra Smart Wellbeing Controls
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wellbeing_controls_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "स्मार्ट वेलबीइंग कंट्रोल" else "Smart Wellbeing Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "माइंडफुल ब्रेक अलर्ट" else "Mindful Break Alerts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHindi) "लिमिट पूरा होने पर स्क्रीन पर ब्रेक संदेश दिखाएं" else "Pop alert when limit is reached",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBreakAlertEnabled,
                            onCheckedChange = { isBreakAlertEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "बेडटाइम मोड (10:30 PM के बाद)" else "Bedtime Mode (10:30 PM)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHindi) "रात को YouTube और सोशल ऐप्स लॉक रखें" else "Block distracting apps late at night",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBedtimeEnabled,
                            onCheckedChange = { isBedtimeEnabled = it }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Dialog for Setting Limits
    appToEditLimit?.let { app ->
        AppLimitDialog(
            appName = app.appName,
            currentLimitMinutes = app.dailyLimitMinutes,
            isHindi = isHindi,
            onDismiss = { appToEditLimit = null },
            onConfirm = { minutes ->
                viewModel.setAppLimit(app.packageName, minutes)
                appToEditLimit = null
            }
        )
    }
}
