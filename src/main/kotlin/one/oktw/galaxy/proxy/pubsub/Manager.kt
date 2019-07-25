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

    private val queues: HashMap<String, String> = HashMap()
    private val tags: HashMap<String, String> = HashMap()
    private val mqOpts: HashMap<String, Any> = HashMap<String, Any>().apply { this["x-message-ttl"] = 0 }
    private val instanceId: UUID = UUID.randomUUID()

    init {
    }

    fun subscribe(topic: String) {
        if (queues[topic] != null) return

        channel.exchangeDeclare("$exchange-$topic", "fanout")
        val queue = channel.queueDeclare().queue

        channel.queueBind(queue, "$exchange-$topic", "")

        queues[topic] = queue
        tags[topic] = channel.basicConsume(queue, ConsumerWrapper(topic, this))
    }

    fun unsubscribe(topic: String) {
        if (queues[topic] == null) return
        if (tags[topic] == null) return

        channel.queueUnbind(queues[topic], "$exchange-$topic", "")
        channel.basicCancel(tags[topic])

        queues.remove(topic)
        tags.remove(topic)
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
