package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AdBlockVpnService
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.ColorTimeWarning
import com.example.viewmodel.MainViewModel

@Composable
fun AdBlockerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    val adSkipEnabled by viewModel.adSkipEnabled.collectAsState()
    val muteAdsEnabled by viewModel.muteAdsEnabled.collectAsState()
    val continueWatchingEnabled by viewModel.continueWatchingEnabled.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()

    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val adsBlockedDnsCount by viewModel.adsBlockedDnsCount.collectAsState()
    val adsSkippedCount by viewModel.adsSkippedCount.collectAsState()

    // Activity launcher for VpnService.prepare
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AdBlockVpnService.start(context)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi) "ऐड ब्लॉकर और स्किप इंजन" else "Ad Blocker & Skip Engine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi)
                    "YouTube वीडियो विज्ञापनों को ऑटो-स्किप करें और बाकी ऐप्स में DNS ऐड ब्लॉक ऑन करें।"
                else
                    "Auto-skip YouTube video ads and filter ad networks across apps using local DNS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: YouTube Video Ad Skipper
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("youtube_ad_skip_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "YouTube ऑटो ऐड स्किप" else "YouTube Auto Ad-Skip",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAccessibilityActive) {
                                        if (isHindi) "एक्सेसिबिलिटी सक्रिय है" else "Accessibility Active"
                                    } else {
                                        if (isHindi) "एक्सेसिबिलिटी बंद है" else "Accessibility Inactive"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAccessibilityActive) ColorAdBlocked else ColorTimeWarning
                                )
                            }
                        }

                        Switch(
                            checked = adSkipEnabled,
                            onCheckedChange = { viewModel.setAdSkipEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorAdBlocked
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Accessibility direct button if not active
                    if (!isAccessibilityActive) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isHindi)
                                        "YouTube में 'Skip Ad' बटन को अपने आप दबाने के लिए Android Accessibility ऑन करें:"
                                    else
                                        "Enable Accessibility so FocusShield can detect & click 'Skip Ad' in 0ms:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Accessibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHindi) "Accessibility सेटिंग्स खोलें" else "Open Accessibility Settings")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Setting item: Mute audio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "अनस्किपेबल विज्ञापनों का म्यूट" else "Mute Unskippable Ads",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHindi)
                                        "जब गैर-स्किप ऐड चले, तो आवाज़ अपने आप म्यूट होगी और वीडियो आते ही वापस आएगी"
                                    else
                                        "Mutes volume during non-skippable ads and restores it when video resumes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = muteAdsEnabled,
                            onCheckedChange = { viewModel.setMuteAdsEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Setting item: Continue watching
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "ऑटो 'Continue Watching'" else "Auto-Confirm 'Continue Watching'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isHindi)
                                        "जब YouTube पूछे 'Still watching?', तो अपने आप 'Yes' क्लिक होगा"
                                    else
                                        "Auto-clicks 'Yes' when YouTube asks 'Video paused. Still watching?'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = continueWatchingEnabled,
                            onCheckedChange = { viewModel.setContinueWatchingEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Language support tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = ColorAdBlocked,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi)
                                "भाषाएं: हिन्दी (विज्ञापन छोड़ें), English (Skip Ad), বাংলা, मराठी और अन्य समर्थित हैं"
                            else
                                "Multi-language supported: English, Hindi (विज्ञापन छोड़ें), and 10+ languages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 2: System-wide DNS Ad Blocker (VPN)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dns_adblock_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isVpnActive) ColorAdBlocked.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (isVpnActive) ColorAdBlocked else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "सिस्टम DNS ऐड शील्ड" else "System DNS Ad Shield",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isVpnActive) {
                                        if (isHindi) "सक्रिय • 94.140.14.14 फ़िल्टर" else "Active • AdGuard DNS Filter"
                                    } else {
                                        if (isHindi) "निष्क्रिय (ऑन करने के लिए स्विच दबाएं)" else "Inactive (Tap toggle to start)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isVpnActive) ColorAdBlocked else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isVpnActive,
                            onCheckedChange = { startVpn ->
                                if (startVpn) {
                                    val prepIntent = VpnService.prepare(context)
                                    if (prepIntent != null) {
                                        vpnLauncher.launch(prepIntent)
                                    } else {
                                        AdBlockVpnService.start(context)
                                    }
                                } else {
                                    AdBlockVpnService.stop(context)
                                }
                            },
                            modifier = Modifier.testTag("system_vpn_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorAdBlocked
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isHindi)
                            "यह तकनीक स्थानीय Android VpnService का उपयोग करके ब्राउज़र, गेम्स और अन्य ऐप्स में विज्ञापन सर्विंग सर्वर (जैसे doubleclick, googleads, app-measurement) के DNS अनुरोधों को ब्लॉक करती है।"
                        else
                            "Filters advertising and tracker DNS requests (like DoubleClick, MoPub, AdService) locally across browsers, games, and other apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$adsBlockedDnsCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorAdBlocked
                                )
                                Text(
                                    text = if (isHindi) "ब्लॉक डोमेन" else "Blocked Queries",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "0.0 MB",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isHindi) "डेटा बचाया" else "Ad Data Saved",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Technical How It Works Explanation
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("how_it_works_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "यह कैसे काम करता है? (How It Works)" else "How It Works (Privacy & Safety)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi)
                            "1. कोई रूट (Root) की आवश्यकता नहीं है।\n2. आपका कोई भी व्यक्तिगत डेटा या वीडियो इतिहास कहीं नहीं भेजा जाता।\n3. Accessibility Service केवल 'Skip Ad' बटन को तुरंत क्लिक करने के लिए स्क्रीन के बटन को पहचानती है।\n4. जब विज्ञापन पूरा होता है, तब आपका वीडियो बिना रुकावट जारी रहता है।"
                        else
                            "1. 100% No Root Required.\n2. Zero data collection: all processing happens 100% on your device.\n3. Accessibility Service scans only for the Skip Ad button element in milliseconds.\n4. Local DNS stops banner and analytics ad networks from downloading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
