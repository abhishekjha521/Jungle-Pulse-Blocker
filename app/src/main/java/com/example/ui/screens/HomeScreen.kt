package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.MetricCard
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.ColorTimeWarning
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToScreenTime: () -> Unit,
    onNavigateToAdBlocker: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    val adSkipEnabled by viewModel.adSkipEnabled.collectAsState()
    val muteAdsEnabled by viewModel.muteAdsEnabled.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()

    val adsSkippedCount by viewModel.adsSkippedCount.collectAsState()
    val adsBlockedDnsCount by viewModel.adsBlockedDnsCount.collectAsState()
    val timeSavedSeconds by viewModel.timeSavedSeconds.collectAsState()
    val usageStats by viewModel.usageStats.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()

    val totalScreenTimeMinutes = usageStats.sumOf { it.usageMinutes }
    val formattedScreenTime = "${totalScreenTimeMinutes / 60}h ${totalScreenTimeMinutes % 60}m"
    val formattedTimeSaved = "${timeSavedSeconds / 60}m ${timeSavedSeconds % 60}s"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Graphic Card
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.hero_focus_shield_1794333770915),
                        contentDescription = "FocusShield Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay with quick tag
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x990B132B),
                                        Color(0xE60B132B)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.ic_jungle_pulse_logo),
                                contentDescription = "Jungle Pulse Icon",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "JUNGLE PULSE BLOCKER" else "JUNGLE PULSE BLOCKER",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isHindi) "YouTube 'Skip in' ऑटो-टैप और स्क्रीन टाइम नियंत्रण" else "YouTube 'Skip in' Auto-Tap & Screen Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF67E8F9)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("protection_status_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (adSkipEnabled) ColorAdBlocked.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (adSkipEnabled) Icons.Default.Shield else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (adSkipEnabled) ColorAdBlocked else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "YouTube ऑटो ऐड स्किप" else "YouTube Auto Ad-Skip",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAccessibilityActive) {
                                        if (isHindi) "सक्रिय ('Skip in' एवं विज्ञापनों को स्वतः 0ms में टैप करता है)" else "Live & Active (0ms 'Skip in' auto-tap)"
                                    } else {
                                        if (isHindi) "सेटिंग्स में एक्सेसिबिलिटी चालू करें" else "Tap below to enable in Settings"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAccessibilityActive) ColorAdBlocked else ColorTimeWarning
                                )
                            }
                        }

                        Switch(
                            checked = adSkipEnabled,
                            onCheckedChange = { viewModel.setAdSkipEnabled(it) },
                            modifier = Modifier.testTag("ad_skip_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorAdBlocked
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary switches for audio mute & test skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "गैर-स्किप विज्ञापनों का ऑडियो म्यूट करें" else "Auto-mute unskippable ads",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = muteAdsEnabled,
                            onCheckedChange = { viewModel.setMuteAdsEnabled(it) },
                            modifier = Modifier.testTag("mute_ads_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Simulation Button
                    Button(
                        onClick = { viewModel.simulateAdSkipped() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_skip_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "⚡ टेस्ट करें: 'Skip in' ऑटो-टैप कैसे काम करता है" else "⚡ Test Live 'Skip in' Auto-Tap"
                        )
                    }
                }
            }
        }

        // Metrics Grid (Ads Skipped, Time Saved, Screen Time)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = if (isHindi) "स्किप विज्ञापन" else "Ads Skipped",
                    value = "$adsSkippedCount",
                    subtitle = if (isHindi) "बचाया समय: $formattedTimeSaved" else "Saved: $formattedTimeSaved",
                    icon = Icons.Default.CheckCircle,
                    iconTint = ColorAdBlocked,
                    modifier = Modifier.weight(1f),
                    testTag = "card_ads_skipped"
                )

                MetricCard(
                    title = if (isHindi) "आज का स्क्रीन टाइम" else "Screen Time",
                    value = formattedScreenTime,
                    subtitle = if (isHindi) "शीर्ष ऐप: YouTube" else "Top: YouTube",
                    icon = Icons.Default.HourglassTop,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToScreenTime() },
                    testTag = "card_screen_time"
                )
            }
        }

        // System DNS VPN Protection Quick Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vpn_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isVpnActive) ColorAdBlocked.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (isVpnActive) ColorAdBlocked else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "सिस्टम DNS ऐड फ़िल्टर (सभी ऐप्स)" else "System DNS Ad Filter (All Apps)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHindi) "$adsBlockedDnsCount ट्रैकर और विज्ञापन ब्लॉक किए गए" else "$adsBlockedDnsCount trackers & ad queries blocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onNavigateToAdBlocker() },
                        modifier = Modifier.testTag("manage_vpn_button")
                    ) {
                        Text(if (isHindi) "प्रबंधित करें" else "Manage")
                    }
                }
            }
        }

        // Setup & Permissions Helper Card (if permissions not granted)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_checklist_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "सेटअप गाइड और अनुमतियाँ (Permissions)" else "Setup & Permissions Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi)
                            "• YouTube में विज्ञापनों को अपने आप हटाने के लिए Accessibility सर्विस ऑन करें।\n• स्क्रीन टाइम को ट्रैक करने और डेली लिमिट लगाने के लिए Usage Access ऑन करें।"
                        else
                            "• Enable Accessibility to let FocusShield detect & click Skip Ad automatically in YouTube.\n• Enable Usage Access to view real screen time stats and enforce limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_accessibility_settings"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isHindi) "1. Accessibility" else "1. Accessibility",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_usage_settings"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isHindi) "2. Usage Access" else "2. Usage Access",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // Recent Ad Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "हाल ही में ब्लॉक और स्किप किए गए विज्ञापन" else "Live Ad Protection Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHindi) "लाइव" else "Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorAdBlocked,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(recentEvents.take(4)) { event ->
            val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_event_${event.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ColorAdBlocked.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ColorAdBlocked,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = event.actionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
