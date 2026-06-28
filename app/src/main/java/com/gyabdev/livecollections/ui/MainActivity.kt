/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gyabdev.livecollections.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gyabdev.livecollections.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

// Подключаем наш сервис (проверь, чтобы папка совпадала, если положил в services — добавь .services)
import com.gyabdev.livecollections.ui.TimerInterceptorService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    // Получаем контекст внутри Compose для запуска Activity настроек
    val context = LocalContext.current
    
    // Подписываемся на обновление таймера
    val currentTime by TimerInterceptorService.timerTime.collectAsState()
    val currentBody by TimerInterceptorService.messageBody.collectAsState()
    val messageTime by TimerInterceptorService.mTime.collectAsState()

    Column {
        // Отображаем время таймера Google прямо в приложении
        Text(text = "Осталось времени: $currentTime")

        // Кнопка для отправки пользователя в настройки
        Button(onClick = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                // Добавляем флаг, так как запускаем из контекста приложения, а не из Activity напрямую
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }) {
            Text("Дать разрешение на перехват")
        }
        val currentTimeMs = System.currentTimeMillis()
        val remainingSeconds = (messageTime.ToInt() - currentTimeMs) / 1000
        Text(text="$currentBody")
        Text(text="$remainingSeconds")
    }
}
