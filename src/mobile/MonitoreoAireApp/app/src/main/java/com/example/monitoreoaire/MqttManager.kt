package com.example.monitoreoaire

import android.content.Context
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

object MqttManager {

    private var client: MqttAsyncClient? = null
    private var currentTopic: String? = null
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
                val t = currentTopic ?: return
                try {
                    c.subscribe(t, 0)
                } catch (_: Exception) { }
            }

            override fun connectionLost(cause: Throwable?) { }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.payload?.toString(Charsets.UTF_8) ?: return
                onMessage?.invoke(payload)
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
            c.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) { }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) { }
            })
        } catch (_: Exception) { }
    }

    fun subscribeToDevice(tokenDevice: String) {
        val c = client ?: return
        val topic = MqttConfig.TOPIC_PREFIX + tokenDevice

        if (currentTopic == topic) return

        try {
            currentTopic?.let { if (c.isConnected) c.unsubscribe(it) }
        } catch (_: Exception) { }

        currentTopic = topic

        try {
            if (c.isConnected) c.subscribe(topic, 0)
        } catch (_: Exception) { }
    }
}
