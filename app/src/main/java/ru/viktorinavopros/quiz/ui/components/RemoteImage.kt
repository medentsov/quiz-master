package ru.viktorinavopros.quiz.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted
import java.net.HttpURLConnection
import java.net.URL

private object ImageCache {
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8).toInt()
    private val memory = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(url: String): Bitmap? = memory.get(url)

    fun put(url: String, bitmap: Bitmap) {
        memory.put(url, bitmap)
    }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        get(url)?.let { return@withContext it }
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 12_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36",
                )
                connection.setRequestProperty("Accept", "image/*,*/*;q=0.8")
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) return@runCatching null

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (bounds.outWidth / sample > 1100 || bounds.outHeight / sample > 1100) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } finally {
                runCatching { connection.disconnect() }
            }
        }.getOrNull()?.also { put(url, it) }
    }
}

private sealed interface ImageState {
    data object Loading : ImageState
    data class Ready(val bitmap: Bitmap) : ImageState
    data object Failed : ImageState
}

/**
 * Картинка вопроса. Грузится напрямую по URL с сайта; при ошибке показывает
 * аккуратную заглушку вместо «битой» картинки.
 */
@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var state by remember(url) { mutableStateOf<ImageState>(ImageState.Loading) }

    LaunchedEffect(url) {
        state = ImageState.Loading
        val bitmap = ImageCache.load(url)
        state = if (bitmap != null) ImageState.Ready(bitmap) else ImageState.Failed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            ImageState.Loading -> CircularProgressIndicator(
                color = JeopardyGold,
                strokeWidth = 2.dp,
                modifier = Modifier.fillMaxSize(0.22f),
            )

            is ImageState.Ready -> Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            ImageState.Failed -> Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "🖼", fontSize = 24.sp)
                Text(
                    text = "Картинка не загрузилась",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Откройте вопрос на сайте, если нужна подсказка.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
