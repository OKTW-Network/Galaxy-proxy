package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.rabbitmq.client.ConnectionFactory
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.runBlocking
import one.oktw.galaxy.proxy.event.ChatExchange
import one.oktw.galaxy.proxy.event.PlayerListWatcher
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.pubsub.Manager
import one.oktw.galaxy.proxy.redis.RedisClient
import org.slf4j.Logger


@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main {
    companion object {
        const val MESSAGE_TOPIC = "chat"
        lateinit var main: Main
            private set
    }

    private val kubernetesClient = KubernetesClient()
    private val redisClient = RedisClient()
    private lateinit var playerListWatcher: PlayerListWatcher
    private lateinit var chatExchange: ChatExchange
    lateinit var proxy: ProxyServer
    lateinit var logger: Logger
    lateinit var manager: Manager

    @Inject
    fun init(proxy: ProxyServer, logger: Logger) {
        main = this
        this.proxy = proxy
        this.logger = logger

        val factory = ConnectionFactory()
        factory.host = "galaxy-srv-owo.oktw.tw"
        factory.port = 30885
        factory.username = "user"
        factory.password = "PzcXswxHdv"
        val connection = factory.newConnection()
        val channel = connection.createChannel()
        manager = Manager(channel, "messages")
        manager.subscribe(MESSAGE_TOPIC)

        runBlocking {
            logger.info("Kubernetes Version: ${kubernetesClient.info().gitVersion}")
            logger.info("Redis version: ${redisClient.version()}")
        }

        proxy.channelRegistrar.register(ChatExchange.eventId)
        proxy.channelRegistrar.register(ChatExchange.eventIdResponse)
        logger.info("Galaxy Init!")
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        playerListWatcher = PlayerListWatcher(proxy, redisClient)

        proxy.eventManager.register(this, playerListWatcher)

        chatExchange = ChatExchange(MESSAGE_TOPIC)
    }
}
