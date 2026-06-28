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
import androidx.core.graphics.drawable.IconCompat

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
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: "Таймер"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            val firstLine = textLines?.firstOrNull()?.toString() ?: ""
            val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            
            val collapsedView: RemoteViews? = notification.contentView 
            // Или через публичное поле в современных API:
            // val collapsedView: RemoteViews? = notification.customContentView
            
            // 2. Развёрнутый кастомный экран (обычно там плееры или расширенный трекинг)
            val bigContentView: RemoteViews? = notification.bigContentView
            // Или:
            // val bigContentView: RemoteViews? = notification.customBigContentView
            
            // 3. Экран для Heads-up (всплывающее сверху уведомление)
            val headsUpView: RemoteViews? = notification.headsUpView
            var text1: = extractTextFromRemoteViews(collapsedView) ?: extractTextFromRemoteViews(bigContentView) ?: extractTextFromRemoteViews(headsUpView) ?: null
            text1 = text1?.get(0)
            val resultTime = title/*when {
                text.isNotEmpty() && text.any { it.isDigit() } -> text
                firstLine.isNotEmpty() && firstLine.any { it.isDigit() } -> firstLine
                infoText.isNotEmpty() && infoText.any { it.isDigit() } -> infoText
                title.isNotEmpty() && title.any { it.isDigit() } -> title
                else -> "жопа"
            }*/
            updateBody("1. $title \n2. $text\n3. $textLines\n4. $infoText\n5. $subText\n6. $text1")
            val dk = notification.`when` ?: 0
            updatenTime(dk)
            if (resultTime != null) {
                val cleanTime = resultTime.trim()
                updateTime(cleanTime)
                
                // Каждую секунду вызываем обновление нашего уведомления-клона
                val smallIcon = notification.smallIcon // Получаем объект android.graphics.drawable.Icon
                val iconCompat: IconCompat? = smallIcon?.let { IconCompat.createFromIcon(this, it) }
                
                showCloneNotification(text1, cleanTime, iconCompat, collapsedView ?: bigContentView ?: headsUpView)
            }
        }
    }

    private fun showCloneNotification(title: String, time: String, smallIcon: IconCompat?, originalRemoteView: RemoteViews?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$title") // Пометка, чтобы ты отличил его от оригинала
            .setContentText(time)             // Сюда каждую секунду залетает новое время
            .setSmallIcon(smallIcon ?: IconCompat.createWithResource(this, android.R.drawable.ic_lock_idle_alarm)) // Иконка будильника/таймера
            .setPriority(NotificationCompat.PRIORITY_LOW)        // Чтобы телефон не вибрировал каждую секунду
            .setOnlyAlertOnce(true)           // КРИТИЧЕСКИ ВАЖНО: обновляет текст без звука и вибрации
            .setOngoing(true)                 // Нельзя смахнуть пальцем, пока идет таймер
            .setAutoCancel(false)
            .setShortCriticalText("$title")
        
        if (originalRemoteView != null) {
            builder.setStyle(androidx.core.app.NotificationCompat.[span_4](start_span)DecoratedCustomViewStyle()) // Обязательно для кастомных вью[span_4](end_span)
            builder.[span_5](start_span)setCustomContentView(originalRemoteView) // Подсовываем перехваченный экран[span_5](end_span)
            
            // Если был большой экран, можно прокинуть и его
            // builder.[span_6](start_span)setCustomBigContentView(originalBigRemoteView)[span_6](end_span)
        }

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
    
    private fun extractTextFromRemoteViews(remoteViews: RemoteViews?): List<String> {
        val extractedTexts = mutableListOf<String>()
        if (remoteViews == null) return extractedTexts
    
        try {
            // 1. Получаем доступ к приватному полю mActions
            val actionsField: Field = remoteViews.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            val actions = actionsField.get(remoteViews) as? List<*> ?: return extractedTexts
    
            // 2. Перебираем все действия, запланированные для этой вьюхи
            for (action in actions) {
                if (action == null) continue
                
                // Ищем действия, которые устанавливают текст (ReflectionAction или аналогичные внутренние классы)
                val actionClass = action.javaClass
                
                // Проверяем, есть ли у этого действия метод или поле "value" (где хранится текст)
                try {
                    // В зависимости от версии Android имя поля может слегка отличаться, 
                    // обычно значение лежит в поле "value" у ReflectionAction
                    val valueField = actionClass.getDeclaredField("value")
                    valueField.isAccessible = true
                    val value = valueField.get(action)
    
                    if (value is CharSequence) {
                        extractedTexts.add(value.toString())
                    }
                } catch (e: NoSuchFieldException) {
                    // Если поля value нет, проверяем другие текстовые свойства
                    continue
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Что-то пошло не так (например, Google изменил внутренности RemoteViews в Android 16)
        }
    
        return extractedTexts
    }
}
