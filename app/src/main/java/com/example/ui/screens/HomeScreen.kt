package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.ColorTimeWarning
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    val adSkipEnabled by viewModel.adSkipEnabled.collectAsState()
    val muteAdsEnabled by viewModel.muteAdsEnabled.collectAsState()
    val adsSkippedCount by viewModel.adsSkippedCount.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Badge Image
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F2618)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_jungle_pulse_logo),
                contentDescription = "Jungle Pulse Logo",
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
            )
        }

        // Title & Tagline
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "JUNGLE PULSE BLOCKER",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi)
                    "YouTube विज्ञापन स्वतः स्किप और 'Skip in' ऑटो-टैप"
                else
                    "YouTube Auto Ad-Skipper & 'Skip in' Auto-Tap",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorAdBlocked,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        // Main Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAccessibilityActive) {
                    ColorAdBlocked.copy(alpha = 0.12f)
                } else {
                    ColorTimeWarning.copy(alpha = 0.12f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                                    if (isAccessibilityActive) ColorAdBlocked.copy(alpha = 0.2f) else ColorTimeWarning.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAccessibilityActive) Icons.Default.Shield else Icons.Default.PlayCircleOutline,
                                contentDescription = null,
                                tint = if (isAccessibilityActive) ColorAdBlocked else ColorTimeWarning,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "YouTube ऐड-स्किपर" else "YouTube Ad Skipper",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAccessibilityActive) {
                                    if (isHindi) "🟢 चालू (Active - 0ms में स्किप)" else "🟢 Active (0ms Auto-Skip)"
                                } else {
                                    if (isHindi) "🔴 बंद (Accessibility चालू करें)" else "🔴 Disabled (Turn ON below)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAccessibilityActive) ColorAdBlocked else ColorTimeWarning,
                                fontWeight = FontWeight.SemiBold
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

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ColorAdBlocked,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "कुल स्किप किए गए विज्ञापन:" else "Total Ads Skipped:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "$adsSkippedCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorAdBlocked
                    )
                }
            }
        }

        // Action Buttons: Enable Accessibility
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "🛠️ सर्विस चालू करने के स्टेप्स:" else "🛠️ How to Enable Skip Service:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHindi)
                        "1. नीचे 'Accessibility चालू करें' पर दबाएं।\n2. 'Jungle Pulse YouTube Skip' को चुनें और ON कर दें।\n3. YouTube खोलें — विज्ञापन आते ही अपने आप स्किप हो जाएंगे!"
                    else
                        "1. Tap 'Turn ON Accessibility' below.\n2. Tap 'Jungle Pulse YouTube Skip' and toggle it ON.\n3. Open YouTube — ads will skip automatically!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Accessibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "Accessibility सेटिंग्स खोलें (Turn ON)" else "Turn ON Accessibility",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Restricted Setting Unlock Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ColorTimeWarning.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ColorTimeWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "यदि 'Restricted setting' दिखे तो (Infinix/Tecno):" else "If 'Restricted setting' appears:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorTimeWarning
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isHindi)
                        "नीचे बटन से App Info खोलें ➔ ऊपर दाईं तरफ 3 डॉट्स (⋮) दबाएं ➔ 'Allow restricted settings' दबाएं!"
                    else
                        "Tap button below to open App Info ➔ Tap 3 dots (⋮) top-right ➔ Tap 'Allow restricted settings'!",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isHindi) "🔓 App Info खोलें (3 डॉट्स ⋮ दबाएं)" else "🔓 Open App Info (Tap 3 dots ⋮)")
                }
            }
        }

        // Switches Card: Auto Mute & Test Skip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                                text = if (isHindi) "विज्ञापन ऑडियो म्यूट करें" else "Auto-Mute Ad Audio",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isHindi) "गैर-स्किप विज्ञापनों में आवाज़ बंद रखें" else "Mute sound on unskippable ads",
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

                // Test Skip Button
                Button(
                    onClick = { viewModel.simulateTestSkip() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_skip_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorAdBlocked)
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "⚡ टेस्ट करें: ऐड स्किप कैसे काम करता है" else "⚡ Test Live Ad Skip"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
