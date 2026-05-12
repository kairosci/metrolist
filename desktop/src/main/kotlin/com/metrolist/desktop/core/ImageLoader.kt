package com.metrolist.desktop.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import com.metrolist.innertube.InternalLogger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

val httpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 15000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 15000
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 2)
        exponentialDelay()
    }
    engine {
        requestTimeout = 15_000
        maxConnectionsCount = 8
    }
}

// Simple LRU cache for images
private object ImageCache {
    private val cache = LinkedHashMap<String, ByteArray>(16, 0.75f, true)
    private val maxSize = 100 * 1024 * 1024 // 100 MB
    private var currentSize = 0L

    fun get(url: String): ByteArray? = synchronized(cache) {
        cache[url]
    }

    fun put(url: String, bytes: ByteArray) = synchronized(cache) {
        // Remove old entries if needed
        while (currentSize + bytes.size > maxSize && cache.isNotEmpty()) {
            val oldest = cache.entries.first()
            cache.remove(oldest.key)
            currentSize -= oldest.value.size
        }
        // Add new entry
        cache[url] = bytes
        currentSize += bytes.size
    }

    fun clear() = synchronized(cache) {
        cache.clear()
        currentSize = 0
    }
}

@Composable
fun rememberUrlImageBitmap(url: String?): ImageBitmap? {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url != null) {
            bitmap = loadBitmap(url)
        }
    }

    return bitmap
}

private suspend fun loadBitmap(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        // Try to get from cache first
        var bytes = ImageCache.get(url)
        
        // If not in cache, download
        if (bytes == null) {
            val response = httpClient.get(url)
            bytes = response.bodyAsBytes()
            ImageCache.put(url, bytes)
        }
        
        ByteArrayInputStream(bytes).use { stream ->
            loadImageBitmap(stream)
        }
    } catch (e: Exception) {
        InternalLogger.e("Failed to load image: $url", e)
        null
    }
}

@Composable
fun rememberUrlImagePainter(url: String?): Painter? {
    val bitmap = rememberUrlImageBitmap(url)
    return bitmap?.let { BitmapPainter(it) }
}
