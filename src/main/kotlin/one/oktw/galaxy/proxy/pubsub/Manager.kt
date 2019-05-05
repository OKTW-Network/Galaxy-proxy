package one.oktw.galaxy.proxy.pubsub

import com.rabbitmq.client.*
import one.oktw.galaxy.proxy.Main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.event.MessageDeliveryEvent

@Suppress("MemberVisibilityCanBePrivate")
class Manager(private val channel: Channel, val exchange: String) {
    companion object {
        class ConsumerWrapper(val topic: String, val manager: Manager) : Consumer {
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
                manager.handleDelivery(topic, consumerTag, envelope, properties, body)
                manager.channel.basicAck(envelope.deliveryTag, false)
            }

            override fun handleCancelOk(consumerTag: String?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    private val tags: HashMap<String, String> = HashMap()
    private val mqOpts: HashMap<String, Any> = HashMap<String, Any>().apply { this["x-message-ttl"] = 0 }


    init {
        channel.exchangeDeclare(exchange, "fanout")
    }

    fun subscribe(topic: String) {
        if (tags[topic] != null) return
        channel.queueDeclare(topic, false, false, false, mqOpts)
        tags[topic] = channel.basicConsume(topic, ConsumerWrapper(topic, this))
    }

    fun unsubscribe(topic: String) {
        if (tags[topic] == null) return
        channel.queueDeclare(topic, false, false, false, mqOpts)
        channel.basicCancel(tags[topic])
    }

    fun handleDelivery(
        topic: String,
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: ByteArray
    ) {
        val message = MessageDeliveryEvent(
            topic,
            consumerTag,
            envelope,
            properties,
            body
        )

        Main.main.proxy.eventManager.fireAndForget(message)
    }

    fun send(topic: String, item: Any) {
        val encoded = ProxyAPI.encode(item)
        send(topic, encoded)
    }

    fun send(topic: String, body: ByteArray) {
        channel.queueDeclare(topic, false, false, false, mqOpts)
        channel.basicPublish("", topic, null, body)
    }
}
