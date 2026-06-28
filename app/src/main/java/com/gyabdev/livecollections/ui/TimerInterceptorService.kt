package com.gyabdev.livecollections.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerInterceptorService : NotificationListenerService() {

    companion object {
        private val _timerTime = MutableStateFlow("Таймер не запущен")
        val timerTime: StateFlow<String> = _timerTime
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Перехватываем часы Google
        if (sbn.packageName == "com.google.android.deskclock") {
            val extras = sbn.notification.extras
            val timerText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (timerText.isNotEmpty()) {
                // Отправляем время в наш StateFlow, который слушает Compose UI
                _timerTime.value = timerText
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == "com.google.android.deskclock") {
            _timerTime.value = "Таймер остановлен"
        }
    }
}
