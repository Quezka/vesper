package io.github.quezka.vesper.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.quezka.vesper.domain.NoticeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reads the phone's notifications (the user grants "notification access") and keeps the current list in [items]. */
class NoticeFeed : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = refresh()
    override fun onListenerDisconnected() {
        _items.value = emptyList()
    }

    private fun refresh() {
        val now = try {
            activeNotifications.orEmpty()
        } catch (e: SecurityException) {
            emptyArray()
        }
        _items.value = now.filter { it.packageName != packageName }.map { toItem(it) }
    }

    private fun toItem(sbn: StatusBarNotification): NoticeItem {
        val n = sbn.notification
        val extras = n.extras
        val app = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }
        return NoticeItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = app,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
                ?.toString().orEmpty(),
            postedAt = sbn.postTime,
            ongoing = sbn.isOngoing,
            groupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        )
    }

    companion object {
        private val _items = MutableStateFlow<List<NoticeItem>>(emptyList())
        val items: StateFlow<List<NoticeItem>> = _items.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val me = ComponentName(context, NoticeFeed::class.java).flattenToString()
            return flat?.split(':')?.contains(me) == true
        }
    }
}
