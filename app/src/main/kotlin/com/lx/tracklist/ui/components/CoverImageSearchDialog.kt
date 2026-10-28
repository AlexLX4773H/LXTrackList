package com.lx.tracklist.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXSurfaceVariant
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import java.net.URLEncoder

enum class SearchEngine(val displayName: String, val buildUrl: (String) -> String) {
    GOOGLE(
        "Google",
        { q -> "https://www.google.com/search?tbm=isch&q=${URLEncoder.encode(q, "UTF-8")}" }
    ),
    DUCKDUCKGO(
        "DuckDuckGo",
        { q -> "https://duckduckgo.com/?q=${URLEncoder.encode(q, "UTF-8")}&iar=images&iax=images&ia=images" }
    ),
    BING(
        "Bing",
        { q -> "https://www.bing.com/images/search?q=${URLEncoder.encode(q, "UTF-8")}" }
    )
}

class ImagePickerBridge(private val onImageSelected: (String) -> Unit) {
    @JavascriptInterface
    fun onImageClicked(url: String) {
        if (url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:image/"))) {
            onImageSelected(url)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoverImageSearchDialog(
    initialQuery: String,
    onImageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedEngine by remember { mutableStateOf(SearchEngine.GOOGLE) }
    var currentWebUrl by remember { mutableStateOf(selectedEngine.buildUrl(initialQuery)) }
    var candidateImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = LXSurfaceBlack,
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header & Search Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Web Cover Search",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = LXTextSecondary)
                        }
                    }

                    // Search Query Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search images...", color = LXTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LXPrimaryGreen,
                                unfocusedBorderColor = LXBorder,
                                focusedTextColor = LXTextPrimary,
                                unfocusedTextColor = LXTextPrimary,
                                cursorColor = LXPrimaryGreen
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary)
                                    }
                                }
                            }
                        )

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    currentWebUrl = selectedEngine.buildUrl(searchQuery)
                                    webViewInstance?.loadUrl(currentWebUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Search Engine Chips & Tip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SearchEngine.entries.forEach { engine ->
                                FilterChip(
                                    selected = selectedEngine == engine,
                                    onClick = {
                                        selectedEngine = engine
                                        if (searchQuery.isNotBlank()) {
                                            currentWebUrl = engine.buildUrl(searchQuery)
                                            webViewInstance?.loadUrl(currentWebUrl)
                                        }
                                    },
                                    label = { Text(engine.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LXPrimaryGreen,
                                        selectedLabelColor = LXBackgroundBlack,
                                        containerColor = LXSurfaceVariant,
                                        labelColor = LXTextSecondary
                                    )
                                )
                            }
                        }

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LXPrimaryGreen)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(LXPrimaryGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "💡 Tap or hold any image below to preview and select it as cover art",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXPrimaryGreen
                        )
                    }
                }

                // Browser Area (WebView)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                val bridge = ImagePickerBridge { url ->
                                    candidateImageUrl = url
                                }
                                addJavascriptInterface(bridge, "AndroidBridge")

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        // Inject touch/click handler for all images on page
                                        val js = """
                                            javascript:(function() {
                                                document.addEventListener('click', function(e) {
                                                    var target = e.target;
                                                    while (target && target.tagName !== 'IMG') {
                                                        target = target.parentElement;
                                                    }
                                                    if (target && target.src) {
                                                        AndroidBridge.onImageClicked(target.src);
                                                    }
                                                }, true);
                                            })()
                                        """.trimIndent()
                                        view?.evaluateJavascript(js, null)
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        return false
                                    }
                                }

                                setOnTouchListener { v, event ->
                                    if (event.action == MotionEvent.ACTION_UP) {
                                        val hit = hitTestResult
                                        val type = hit.type
                                        if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                                            val extra = hit.extra
                                            if (!extra.isNullOrBlank()) {
                                                candidateImageUrl = extra
                                            }
                                        }
                                    }
                                    false
                                }

                                loadUrl(currentWebUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Candidate Image Selection Bar
                if (candidateImageUrl != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LXSurfaceElevated)
                            .border(width = 1.dp, color = LXPrimaryGreen, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, LXBorder, RoundedCornerShape(6.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = candidateImageUrl,
                                    contentDescription = "Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Selected Image",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXPrimaryGreen
                                )
                                Text(
                                    text = candidateImageUrl ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = LXTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { candidateImageUrl = null }) {
                                Text("Cancel", color = LXTextSecondary)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    candidateImageUrl?.let { onImageSelected(it) }
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use as Cover", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

