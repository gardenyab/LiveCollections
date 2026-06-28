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
import android.widget.RemoteViews
import android.widget.FrameLayout
import android.widget.TextView
import android.view.ViewGroup
import android.view.View

class TimerInterceptorService : NotificationListenerService() {

    private val channelId = "timer_clone_channel"
    private val cloneNotificationId = 8888 // Фиксированный ID для нашего клона

    companion object {
        private val _timerTime = MutableStateFlow("Таймер не запущен")
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
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: "Таймер"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            val firstLine = textLines?.firstOrNull()?.toString() ?: ""
            val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            
            val collapsedView: RemoteViews? = notification.contentView 
            val bigContentView: RemoteViews? = notification.bigContentView
            
            // Используем новый, надежный способ рендеринга лайоута вместо старой рефлексии
            val text1List = getTextViaLayoutRendering(collapsedView).takeIf { it.isNotEmpty() }
                ?: getTextViaLayoutRendering(bigContentView)
            
            val text1 = text1List.firstOrNull() ?: ""
            val resultTime = title

            updateBody("1. $title \n2. $text\n3. $textLines\n4. $infoText\n5. $subText\n6. $text1")
            val dk = notification.`when` ?: 0
            updatenTime(dk)
            
            if (resultTime != null) {
                val cleanTime = resultTime.trim()
                updateTime(cleanTime)
                
                val smallIcon = notification.smallIcon 
                val iconCompat: IconCompat? = smallIcon?.let { IconCompat.createFromIcon(this, it) }
                
                // Достаем первую строку из отрендеренного View (или фолбек "шо"), чтобы передать как String в заголовок клона
                val renderedTitle = getTextViaLayoutRendering(bigContentView ?: collapsedView).firstOrNull() ?: "шо"
                
                showCloneNotification(renderedTitle, cleanTime, iconCompat, collapsedView ?: bigContentView)
            }
        }
    }

    private fun showCloneNotification(title: String, time: String, smallIcon: IconCompat?, originalRemoteView: RemoteViews?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title) 
            .setContentText(time)             
            .setSmallIcon(smallIcon ?: IconCompat.createWithResource(this, android.R.drawable.ic_lock_idle_alarm)) 
            .setPriority(NotificationCompat.PRIORITY_LOW)        
            .setOnlyAlertOnce(true)           
            .setOngoing(true)                 
            .setAutoCancel(false)
            .setShortCriticalText(title)
            .setRequestPromotedOngoing(true)

        notificationManager.notify(cloneNotificationId, builder.build())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == "com.google.android.deskclock") {
            updateTime("Таймер остановлен")
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(cloneNotificationId)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Клон Таймера",
                NotificationManager.IMPORTANCE_LOW 
            ).apply {
                description = "Синхронный клон системного таймера"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    
    fun getTextViaLayoutRendering(remoteViews: RemoteViews?): List<String> {
        val result = mutableListOf<String>()
        if (remoteViews == null) return result
    
        try {
            val context = this 
            val container = FrameLayout(context)
            
            // Накатываем разметку во временный контейнер в памяти
            val inflatedView = remoteViews.apply(context, container)
            
            // Собираем текст
            getAllTextViews(inflatedView, result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
    
    // Исправлена сигнатура: убран синтаксический мусор 'or' и 'ArrayList'
    private fun getAllTextViews(view: View, list: MutableList<String>) {
        if (view is TextView) {
            if (!view.text.isNullOrEmpty()) {
                list.add(view.text.toString())
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                getAllTextViews(view.getChildAt(i), list)
            }
        }
    }
}
