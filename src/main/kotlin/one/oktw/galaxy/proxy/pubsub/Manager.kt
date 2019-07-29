package one.oktw.galaxy.proxy.pubsub

import com.rabbitmq.client.*
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.Packet
import one.oktw.galaxy.proxy.event.MessageDeliveryEvent
import one.oktw.galaxy.proxy.pubsub.data.MessageWrapper
import java.util.*
import kotlin.collections.HashMap

class Manager(private val channel: Channel, private val exchange: String) {
    companion object {
        class ConsumerWrapper(private val topic: String, private val manager: Manager) : Consumer {
            override fun handleRecoverOk(consumerTag: String?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun handleConsumeOk(consumerTag: String?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun handleShutdownSignal(consumerTag: String?, sig: ShutdownSignalException?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun handleCancel(consumerTag: String?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                manager.channel.basicAck(envelope.deliveryTag, false)
                manager.handleDelivery(topic, body)
            }

            override fun handleCancelOk(consumerTag: String?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    private val queueAndTag: HashMap<String, Pair<String, String>> = HashMap()
    private val instanceId: UUID = UUID.randomUUID()

    init {
    }

    fun reSubscribeAll() {
        queueAndTag.entries.forEach { entry ->
            try {
                channel.queueUnbind(entry.value.first, "$exchange-${entry.key}", "")
            } catch (err: Throwable) {
                main.logger.error("Error while unbind old channel", err)
            }

            try {
                channel.basicCancel(entry.value.second)
            } catch (err: Throwable) {
                main.logger.error("Error while unlisten old channel", err)
            }

            channel.exchangeDeclare("$exchange-${entry.key}", "fanout")
            val queue = channel.queueDeclare().queue

            channel.queueBind(queue, "$exchange-${entry.key}", "")

            queueAndTag[entry.key] = queue to channel.basicConsume(queue, ConsumerWrapper(entry.key, this))
        }
    }

    fun subscribe(topic: String) {
        if (queueAndTag[topic] != null) return

        channel.exchangeDeclare("$exchange-$topic", "fanout")
        val queue = channel.queueDeclare().queue

        channel.queueBind(queue, "$exchange-$topic", "")

        queueAndTag[topic] = queue to channel.basicConsume(queue, ConsumerWrapper(topic, this))
    }

    fun unsubscribe(topic: String) {
        if (queueAndTag[topic] == null) return

        channel.queueUnbind(queueAndTag[topic]!!.first, "$exchange-$topic", "")
        channel.basicCancel(queueAndTag[topic]!!.second)

        queueAndTag.remove(topic)
    }

    fun handleDelivery(topic: String, body: ByteArray) {
        val unwrappedData = try {
            ProxyAPI.decode<MessageWrapper>(body)
        } catch (err: Throwable) {
            main.logger.error("Decode MessageWrapper packet fail", err)
            null
        } ?: return

        // drop short circuited message
        if (unwrappedData.source == instanceId) return

        MessageDeliveryEvent(topic, unwrappedData.message)
            .let { main.proxy.eventManager.fireAndForget(it) }
    }

    fun send(topic: String, item: Packet) {
        MessageDeliveryEvent(topic, item)
            .let { main.proxy.eventManager.fireAndForget(it) }

        send(topic, ProxyAPI.encode(MessageWrapper(instanceId, item)))
    }

    fun send(topic: String, body: ByteArray) {
        channel.basicPublish("$exchange-$topic", topic, null, body)
    }
}
