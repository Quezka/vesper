package io.github.quezka.vesper.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import io.github.quezka.vesper.domain.QuireFeed

/** Reads what Quire (installed on the same phone) offers through its bedside provider. */
object QuireLink {
    const val PERMISSION = "io.github.quezka.quire.permission.READ_BEDSIDE"
    private val FEED: Uri = Uri.parse("content://io.github.quezka.quire.bedside/feed")

    sealed interface Result {
        data class Ok(val feed: QuireFeed) : Result
        data object NotInstalled : Result
        data object NotAllowed : Result
    }

    fun installed(context: Context): Boolean =
        context.packageManager.resolveContentProvider("io.github.quezka.quire.bedside", 0) != null

    fun allowed(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Call off the main thread. */
    fun read(context: Context): Result {
        if (!installed(context)) return Result.NotInstalled
        if (!allowed(context)) return Result.NotAllowed
        return try {
            context.contentResolver.query(FEED, null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return Result.NotInstalled
                val row = HashMap<String, Any?>()
                for (i in 0 until c.columnCount) {
                    row[c.getColumnName(i)] = when (c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                        android.database.Cursor.FIELD_TYPE_STRING -> c.getString(i)
                        else -> null
                    }
                }
                Result.Ok(QuireFeed.fromRow(row))
            } ?: Result.NotInstalled
        } catch (e: SecurityException) {
            Result.NotAllowed
        } catch (e: Exception) {
            Result.NotInstalled
        }
    }
}
