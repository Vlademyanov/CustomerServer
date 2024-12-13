package com.vldm.customerserver.model

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString

class WebSocketClient {
    private lateinit var webSocket: WebSocket

    fun connect(url: String, onMessageReceived: (String) -> Unit, onOpen: () -> Unit, onFailure: (Throwable) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Действия при успешном подключении
                onOpen() // Вызов колбэка при успешном подключении
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Обработка входящих текстовых сообщений
                onMessageReceived(text) // Уведомление о новом сообщении
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Обработка ошибок подключения
                onFailure(t) // Вызов колбэка при ошибке
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null) // Закрытие соединения
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Действия при закрытии соединения
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket.send(message)
    }

    fun close() {
        webSocket.close(1000, "Закрытие соединения")
    }
}