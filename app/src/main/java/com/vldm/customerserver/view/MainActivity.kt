package com.vldm.customerserver.view

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vldm.customerserver.R
import com.vldm.customerserver.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var tokenInput: EditText
    private lateinit var sendTokenButton: Button
    private lateinit var messageList: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var connectionStatusTextView: TextView
    private val viewModel: MainViewModel by viewModels()
    private val url = "wss://stream.binance.com"
    

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов UI
        tokenInput = findViewById(R.id.tokenInput)
        sendTokenButton = findViewById(R.id.sendTokenButton)
        messageList = findViewById(R.id.messageList)
        connectionStatusTextView = findViewById(R.id.connectionStatus)

        adapter = MessageAdapter()
        messageList.adapter = adapter
        messageList.layoutManager = LinearLayoutManager(this)

        // Настройка WebSocket
        viewModel.connectToWebSocket(url)

        sendTokenButton.setOnClickListener {
            val message = tokenInput.text.toString()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                tokenInput.text.clear()
            } else {
                Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show()
            }
        }

        // Наблюдение за сообщениями
        viewModel.messages.observe(this, { messages ->
            adapter.submitList(messages) // Обновляем адаптер с новыми сообщениями
            messageList.smoothScrollToPosition(messages.size) // Прокручиваем к последнему сообщению
        })

        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true

        // Устанавливаем WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Загружаем URL внутри WebView
                view?.loadUrl(url ?: "")
                return true // Указываем, что мы обработали это событие
            }
        }

        sendTokenButton.setOnClickListener {
            val token = tokenInput.text.toString().trim()
            if (token.isNotEmpty()) {
                viewModel.closeWebSocket() 
                viewModel.connectToCryptoWebSocket(token) 
            } else {
                Toast.makeText(this, "Введите название токена", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.connectionStatus.observe(this, { status ->
            if (viewModel.getConnectionStatus()) {
                connectionStatusTextView.text = status
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.closeWebSocket() // Закрытие WebSocket при выходе
    }
}