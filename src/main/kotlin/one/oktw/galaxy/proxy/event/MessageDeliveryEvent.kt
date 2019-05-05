package one.oktw.galaxy.proxy.event

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Envelope
import one.oktw.galaxy.proxy.api.ProxyAPI

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class MessageDeliveryEvent(
    val topic: String,
    val consumerTag: String,
    val envelope: Envelope,
    val properties: AMQP.BasicProperties?,
    val body: ByteArray
) {
    val data = try { ProxyAPI.decode<Any>(body) } catch (err: Throwable) { null }
}
