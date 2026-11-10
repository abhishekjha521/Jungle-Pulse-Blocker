package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.ColorAdBlocked
import com.example.ui.theme.ColorLimitExceeded
import com.example.ui.theme.ColorTimeWarning
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DistractionFreePlayerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    var currentUrl by remember { mutableStateOf("https://m.youtube.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Built-in player session timer
    var watchSeconds by remember { mutableIntStateOf(0) }
    val sessionLimitMinutes = 30
    val sessionLimitSeconds = sessionLimitMinutes * 60

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            watchSeconds++
        }
    }

    val watchMinutes = watchSeconds / 60
    val isOverLimit = watchSeconds >= sessionLimitSeconds
    val progress = (watchSeconds.toFloat() / sessionLimitSeconds.toFloat()).coerceIn(0f, 1f)

    // Ad-blocking JavaScript and CSS injected into web YouTube
    val adBlockScript = """
        (function() {
            // CSS to hide ad banners & overlays
            var style = document.createElement('style');
            style.innerHTML = '
                .ytp-ad-overlay-container,
                .ytp-ad-message-container,
                .video-ads,
                .ytp-ad-module,
                ytd-promoted-sparkles-web-renderer,
                ytd-ad-slot-renderer,
                ytd-promoted-video-renderer,
                #player-ads { display: none !important; }
            ';
            document.head.appendChild(style);

            // Auto-click skip button every 300ms
            setInterval(function() {
                var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .videoAdUiSkipButton');
                if (skipBtn) {
                    skipBtn.click();
                }
                var closeOverlay = document.querySelector('.ytp-ad-overlay-close-button');
                if (closeOverlay) {
                    closeOverlay.click();
                }
            }, 300);
        })();
    """.trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("distraction_free_player_screen")
    ) {
        // Top Header with Ad-Free Badge and Screen Time Watch Timer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ColorAdBlocked.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ColorAdBlocked,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "100% ऐड-फ्री प्लेयर" else "100% Ad-Free Player",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorAdBlocked,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (isOverLimit) ColorLimitExceeded else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "$watchMinutes / $sessionLimitMinutes मिनट" else "$watchMinutes / ${sessionLimitMinutes}m limit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverLimit) ColorLimitExceeded else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (isOverLimit) ColorLimitExceeded else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        // Limit Warning Banner inside player
        AnimatedVisibility(visible = isOverLimit) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ColorLimitExceeded)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi)
                                "समय सीमा समाप्त! आपने 30 मिनट YouTube देख लिया है। कृपया ब्रेक लें।"
                            else
                                "Time limit reached! You've watched 30 min. Take a mindful break.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { watchSeconds = 0 },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "+10 मिनट" else "+10m")
                    }
                }
            }
        }

        // Webview embed
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // Inject ad-blocking script
                                view?.evaluateJavascript(adBlockScript, null)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: ""
                                // Block known web ad network requests
                                if (reqUrl.contains("googleads") || reqUrl.contains("doubleclick") ||
                                    reqUrl.contains("pagead") || reqUrl.contains("adservice")
                                ) {
                                    return WebResourceResponse("text/plain", "utf-8", null)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = {
                    webViewInstance = it
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
