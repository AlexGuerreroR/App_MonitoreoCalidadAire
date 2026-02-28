package com.example.monitoreoaire

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

object MqttManager {

    private var client: MqttAsyncClient? = null
    private val subscribedTopics = mutableSetOf<String>()
    private var onMessage: ((String) -> Unit)? = null

    fun setOnMessageListener(listener: (String) -> Unit) {
        onMessage = listener
    }

    fun init(context: Context) {
        if (client != null) return

        val clientId = "AIRE_APP_" + UUID.randomUUID().toString()
        val c = MqttAsyncClient(MqttConfig.BROKER_URI, clientId, MemoryPersistence())
        client = c

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                subscribedTopics.forEach { topic ->
                    try {
                        c.subscribe(topic, 0)
                    } catch (_: Exception) { }
                }
            }

            override fun connectionLost(cause: Throwable?) { }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                // Solo procesamos si el tópico está en nuestra lista actual de interés
                if (subscribedTopics.contains(topic)) {
                    val payload = message?.payload?.toString(Charsets.UTF_8) ?: return
                    onMessage?.invoke(payload)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) { }
        })

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 10
            keepAliveInterval = 30
        }

        try {
            c.connect(options, null, null)
        } catch (_: Exception) { }
    }

    fun clearAllSubscriptions() {
        val c = client ?: return
        subscribedTopics.forEach { topic ->
            try {
                if (c.isConnected) c.unsubscribe(topic)
            } catch (_: Exception) { }
        }
        subscribedTopics.clear()
    }

    fun subscribeToDevice(tokenDevice: String) {
        val c = client ?: return
        val topic = MqttConfig.TOPIC_PREFIX + tokenDevice

        if (subscribedTopics.contains(topic)) return

        subscribedTopics.add(topic)

        try {
            if (c.isConnected) {
                c.subscribe(topic, 0)
            }
        } catch (_: Exception) { }
    }

    fun unsubscribeFromDevice(tokenDevice: String) {
        val c = client ?: return
        val topic = MqttConfig.TOPIC_PREFIX + tokenDevice

        try {
            if (c.isConnected && subscribedTopics.contains(topic)) {
                c.unsubscribe(topic)
            }
            subscribedTopics.remove(topic)
        } catch (_: Exception) { }
    }
}