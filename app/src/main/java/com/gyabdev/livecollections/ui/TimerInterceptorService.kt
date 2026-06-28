package com.gyabdev.livecollections.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerInterceptorService : NotificationListenerService() {

    companion object {
        // Используем инкапсуляцию, чтобы избежать сбоев при обращении к потоку данных
        private val _timerTime = MutableStateFlow("Таймер не запущен")
        val timerTime: StateFlow<String> = _timerTime.asStateFlow()
        
        // Функция для безопасного обновления данных из любой точки приложения
        fun updateTime(newTime: String) {
            _timerTime.value = newTime
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // Безопасная проверка: если данных нет или сервис еще не готов — просто выходим, без краша
        val packageName = sbn?.packageName ?: return

        if (packageName == "com.google.android.deskclock") {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return
            
            val timerText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            if (!timerText.isNullOrEmpty()) {
                updateTime(timerText)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == "com.google.android.deskclock") {
            updateTime("Таймер остановлен")
        }
    }
}
