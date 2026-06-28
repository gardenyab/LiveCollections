package com.gyabdev.livecollections.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerInterceptorService : NotificationListenerService() {

    private val channelId = "timer_clone_channel"
    private val cloneNotificationId = 8888 // Фиксированный ID для нашего клона

    companion object {
        private val _timerTime = MutableStateFlow("Тайmer не запущен")
        val timerTime: StateFlow<String> = _timerTime.asStateFlow()
        private val _messageBody = MutableStateFlow("пусто пока")
        val messageBody: StateFlow<String> = _messageBody.asStateFlow()
        private val _messageTime = MutableStateFlow(0L)
        val mTime: StateFlow<Long> = _messageTime.asStateFlow()
        
        fun updateTime(newTime: String) {
            _timerTime.value = newTime
        }
        fun updateBody(nbody: String) {
            _messageBody.value = nbody
        }
        fun updatenTime(nbody: Long) {
            _messageTime.value = nbody
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: return

        if (true) {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return
            
            // Наш парсер текста из прошлого шага
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Таймер"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            val firstLine = textLines?.firstOrNull()?.toString() ?: ""
            val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            

            val resultTime = title/*when {
                text.isNotEmpty() && text.any { it.isDigit() } -> text
                firstLine.isNotEmpty() && firstLine.any { it.isDigit() } -> firstLine
                infoText.isNotEmpty() && infoText.any { it.isDigit() } -> infoText
                title.isNotEmpty() && title.any { it.isDigit() } -> title
                else -> "жопа"
            }*/
            updateBody("1. $title \n2. $text\n3. $textLines\n4. $infoText\n5. $subText")
            val dk = notification.`when` ?: 0
            updatenTime(dk)
            if (resultTime != null) {
                val cleanTime = resultTime.trim()
                updateTime(cleanTime)
                
                // Каждую секунду вызываем обновление нашего уведомления-клона
                val iconDrawable: Drawable? = smallIcon?.loadDrawable(this)
                
                showCloneNotification(title, cleanTime, iconDrawable)
            }
        }
    }

    private fun showCloneNotification(title: String, time: String, smallIcon: Drawable?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("[Клон] $title") // Пометка, чтобы ты отличил его от оригинала
            .setContentText(time)             // Сюда каждую секунду залетает новое время
            .setSmallIcon(smallIcon ?: android.R.drawable.ic_lock_idle_alarm) // Иконка будильника/таймера
            .setPriority(NotificationCompat.PRIORITY_LOW)        // Чтобы телефон не вибрировал каждую секунду
            .setOnlyAlertOnce(true)           // КРИТИЧЕСКИ ВАЖНО: обновляет текст без звука и вибрации
            .setOngoing(true)                 // Нельзя смахнуть пальцем, пока идет таймер
            .setAutoCancel(false)

        // notify() с одним и тем же ID (8888) не создает новое уведомление, а обновляет старое
        notificationManager.notify(cloneNotificationId, builder.build())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == "com.google.android.deskclock") {
            updateTime("Таймер остановлен")
            
            // Если оригинальный таймер удалили/выключили — удаляем и наш клон
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(cloneNotificationId)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Клон Таймера",
                NotificationManager.IMPORTANCE_LOW // Низкий приоритет, чтобы не спамить звуками
            ).apply {
                description = "Синхронный клон системного таймера"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
