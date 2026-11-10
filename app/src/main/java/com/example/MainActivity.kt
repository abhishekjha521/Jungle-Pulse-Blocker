package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdBlockerScreen
import com.example.ui.screens.DistractionFreePlayerScreen
import com.example.ui.screens.FocusModeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScreenTimeScreen
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.FocusShieldTheme
import com.example.viewmodel.MainViewModel

enum class NavigationTab(val route: String, val titleEn: String, val titleHi: String, val icon: ImageVector) {
    HOME("home", "Shield", "शील्ड", Icons.Default.Shield),
    SCREEN_TIME("screentime", "Screen Time", "स्क्रीन टाइम", Icons.Default.HourglassTop),
    AD_BLOCK("adblock", "Ad Blocker", "ऐड ब्लॉक", Icons.Default.Block),
    PLAYER("player", "Ad-Free Web", "प्लेयर", Icons.Default.PlayCircleOutline),
    FOCUS("focus", "Focus", "फ़ोकस", Icons.Default.Lock)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusShieldTheme {
                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
                val language by viewModel.language.collectAsState()
                val isHindi = language == "hi"

                val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
                val isVpnActive by viewModel.isVpnActive.collectAsState()
                val feedbackMessage by viewModel.userFeedbackMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Pop custom backstack to Home when back button is pressed on sub-screens
                BackHandler(enabled = currentTab != NavigationTab.HOME) {
                    currentTab = NavigationTab.HOME
                }

                LaunchedEffect(feedbackMessage) {
                    feedbackMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearFeedbackMessage()
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_jungle_pulse_logo),
                                        contentDescription = "Jungle Pulse Logo",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Jungle Pulse",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Shield Live Dot indicator
                                    val isLive = isAccessibilityActive || isVpnActive
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (isLive) ColorAdBlocked else Color.Gray,
                                                CircleShape
                                            )
                                    )
                                }
                            },
                            actions = {
                                // Language Switcher Button (English / हिन्दी)
                                OutlinedButton(
                                    onClick = { viewModel.toggleLanguage() },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .testTag("language_toggle_button"),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Language",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHindi) "हिन्दी" else "EN",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                val label = if (isHindi) tab.titleHi else tab.titleEn
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { targetTab ->
                            when (targetTab) {
                                NavigationTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToScreenTime = { currentTab = NavigationTab.SCREEN_TIME },
                                    onNavigateToAdBlocker = { currentTab = NavigationTab.AD_BLOCK },
                                    onNavigateToPlayer = { currentTab = NavigationTab.PLAYER }
                                )
                                NavigationTab.SCREEN_TIME -> ScreenTimeScreen(viewModel = viewModel)
                                NavigationTab.AD_BLOCK -> AdBlockerScreen(viewModel = viewModel)
                                NavigationTab.PLAYER -> DistractionFreePlayerScreen(viewModel = viewModel)
                                NavigationTab.FOCUS -> FocusModeScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
