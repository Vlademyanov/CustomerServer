package com.vldm.customerserver.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vldm.customerserver.model.Message
import com.vldm.customerserver.model.WebSocketClient
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject

class MainViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    private val _connectionStatus = MutableLiveData<String>()
    val connectionStatus: LiveData<String> get() = _connectionStatus

    private val webSocketClient = WebSocketClient()

    private var lastMessageTime: Long = 0
    private val messageDelay: Long = 500
    private var hasAttemptedConnection = false

    fun connectToWebSocket(url: String) {
        webSocketClient.connect(url,
            { message ->
                // Обработка входящего сообщения
                val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
                currentMessages.add(Message(content = message, timestamp = System.currentTimeMillis()))
                _messages.postValue(currentMessages)
            },
            {
                // Действия при успешном подключении
                _connectionStatus.postValue("Подключение успешно")
            },
            { error ->
                // Обработка ошибок подключения
                _connectionStatus.postValue("Ошибка подключения: $error")
            }
        )
    }

    fun sendMessage(message: String) {
        webSocketClient.sendMessage(message)
        // Добавление отправленного сообщения в список
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        currentMessages.add(Message(content = message, timestamp = System.currentTimeMillis()))
        _messages.postValue(currentMessages)
    }

    fun closeWebSocket() {
        webSocketClient.close()
        _connectionStatus.postValue("WebSocket закрыт")
    }

    fun getConnectionStatus(): Boolean {
        return hasAttemptedConnection
    }

    fun connectToCryptoWebSocket(token: String) {
        val url = "wss://stream.binance.com:9443/ws/${token.toLowerCase()}usdt@trade"
        Log.d("MainViewModel", "Подключение к WebSocket: $url")
        webSocketClient.connect(url,
            { message ->
                // Обработка входящего сообщения
                val json = JSONObject(message)

                // Проверяем, что это сообщение типа "trade"
                if (json.has("e") && json.getString("e") == "trade") {
                    // Проверяем, содержит ли сообщение нужные данные
                    if (json.has("p")) { // "p" - это поле с ценой
                        val price = json.getString("p") // Получаем цену
                        val newMessageContent = "Цена $token: $price"
                        val currentTime = System.currentTimeMillis()

                        // Проверяем, прошло ли достаточно времени с последнего обновления
                        if (currentTime - lastMessageTime >= messageDelay) {
                            lastMessageTime = currentTime // Обновляем время последнего сообщения

                            // Проверяем, существует ли уже такое сообщение
                            val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
                            if (currentMessages.isEmpty() || currentMessages.last().content != newMessageContent) {
                                currentMessages.add(Message(content = newMessageContent, timestamp = System.currentTimeMillis()))
                                _messages.postValue(currentMessages)
                                Log.d("MainViewModel", "Добавлено сообщение: $newMessageContent")
                            } else {
                                Log.d("MainViewModel", "Сообщение уже существует: $newMessageContent")
                            }
                        } else {
                            Log.d("MainViewModel", "Сообщение игнорируется, так как прошло недостаточно времени")
                        }
                    }
                }
            },
            {
                if (!hasAttemptedConnection) {
                    hasAttemptedConnection = true // Устанавливаем флаг, что была попытка подключения
                }
                _connectionStatus.postValue("Подключение успешно для токена: $token") // Обновляем статус подключения
            },
            { error ->
                if (!hasAttemptedConnection) {
                    hasAttemptedConnection = true // Устанавливаем флаг, что была попытка подключения
                }
                _connectionStatus.postValue("Ошибка подключения к WebSocket: $error") // Обновляем статус подключения при ошибке
            }
        )
    }
}