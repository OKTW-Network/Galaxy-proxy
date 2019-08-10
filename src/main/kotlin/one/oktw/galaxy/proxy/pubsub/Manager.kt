package one.oktw.galaxy.proxy.pubsub

import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.pubsub.RedisPubSubAdapter
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.Packet
import one.oktw.galaxy.proxy.config.CoreSpec
import one.oktw.galaxy.proxy.event.MessageDeliveryEvent
import one.oktw.galaxy.proxy.pubsub.data.MessageWrapper
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Manager(prefix: String) {
    private val client = RedisClient.create(main.config[CoreSpec.redis])
    private val subscribeConnection = client.connectPubSub(ByteArrayCodec())
    private val publishConnection = client.connect(ByteArrayCodec())
    private val queries: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()
    private val instanceId: UUID = UUID.randomUUID()

    init {
        subscribeConnection.addListener(object : RedisPubSubAdapter<ByteArray, ByteArray>() {
            override fun message(channel: ByteArray, message: ByteArray) {
                handleDelivery(channel.toTopic() ?: return, message)
            }
        })
    }

    private val channelPrefix = "$prefix-chat-"
    private fun ByteArray.toTopic(): String? {
        return String(this).let {
            if (it.startsWith(channelPrefix)) {
                it.drop(channelPrefix.length)
            } else {
                null
            }
        }
    }

    private fun String.toChannel() = "$channelPrefix$this".toByteArray()

    fun subscribe(topic: String) {
        if (queries.contains(topic)) return
        queries[topic] = true
        subscribeConnection.sync().subscribe(topic.toChannel())
    }

    fun unsubscribe(topic: String) {
        if (!queries.contains(topic)) return
        subscribeConnection.sync().unsubscribe(topic.toChannel())
        queries.remove(topic)
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
        main.proxy.eventManager.fireAndForget(MessageDeliveryEvent(topic, item))
        send(topic, ProxyAPI.encode(MessageWrapper(instanceId, item)))
    }

    fun send(topic: String, body: ByteArray) {
        publishConnection.async().publish(topic.toChannel(), body)
    }
}
