package io.github.quezka.vesper.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Launcher icons of the apps that posted notifications, drawn once and kept in memory. */
object AppIcons {
    private const val SIZE = 96
    private val cache = LruCache<String, ImageBitmap>(64)
    private val missing = HashSet<String>()

    fun get(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        if (packageName in missing) return null
        return try {
            context.packageManager.getApplicationIcon(packageName).toBitmap(SIZE, SIZE).asImageBitmap()
                .also { cache.put(packageName, it) }
        } catch (e: Exception) {
            missing += packageName
            null
        }
    }
}
