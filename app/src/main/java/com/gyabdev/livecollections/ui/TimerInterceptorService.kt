package com.gyabdev.livecollections.ui

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerInterceptorService : NotificationListenerService() {

    companion object {
        private val _timerTime = MutableStateFlow("Таймер не запущен")
        val timerTime: StateFlow<String> = _timerTime.asStateFlow()
        
        fun updateTime(newTime: String) {
            _timerTime.value = newTime
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: return

        // Проверяем часы Google
        if (packageName == "com.google.android.deskclock") {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return
            
            // Извлекаем вообще все текстовые поля, которые могут содержать время
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            // Ищем, где именно сейчас находятся цифры таймера (например: "04:59" или "Таймер - 00:15")
            val resultTime = when {
                // Если время попало в основной текст
                text.isNotEmpty() && text.any { it.isDigit() } -> text
                // Если Google засунул время в подтекст (бывает на новых Android)
                subText.isNotEmpty() && subText.any { it.isDigit() } -> subText
                // Если время отображается в заголовке
                title.isNotEmpty() && title.any { it.isDigit() } -> title
                // Резервный вариант из развернутого уведомления
                bigText.isNotEmpty() && bigText.any { it.isDigit() } -> bigText
                else -> null
            }

            if (resultTime != null) {
                // Очистим текст от лишних системных символов, если они прилетят
                updateTime(resultTime.trim())
            } else {
                // Если уведомление пришло, но цифр не нашли — выведем структуру для теста
                updateTime("Поймали: Tl:$title | Tx:$text")
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
