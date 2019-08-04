package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.TopologyRecoveryException
import com.rabbitmq.client.impl.DefaultExceptionHandler
import com.uchuhimo.konf.Config
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.fabric8.kubernetes.client.internal.readiness.Readiness
import kotlinx.coroutines.*
import one.oktw.galaxy.proxy.command.Lobby
import one.oktw.galaxy.proxy.config.CoreSpec
import one.oktw.galaxy.proxy.config.GalaxySpec
import one.oktw.galaxy.proxy.config.GalaxySpec.Storage.storageClass
import one.oktw.galaxy.proxy.event.ChatExchange
import one.oktw.galaxy.proxy.event.GalaxyPacket
import one.oktw.galaxy.proxy.event.PlayerListWatcher
import one.oktw.galaxy.proxy.event.TabListUpdater
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.pubsub.Manager
import one.oktw.galaxy.proxy.redis.RedisClient
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main {
    companion object {
        const val MESSAGE_TOPIC = "chat"
        lateinit var main: Main
            private set
    }

    val config: Config

    lateinit var lobby: RegisteredServer
        private set
    lateinit var kubernetesClient: KubernetesClient
        private set
    lateinit var redisClient: RedisClient
        private set
    lateinit var proxy: ProxyServer
        private set
    lateinit var logger: Logger
        private set
    lateinit var chatExchange: ChatExchange
        private set

    init {
        Files.createDirectories(Paths.get("config"))
        if (!Files.exists(Paths.get("config", "galaxy-proxy.toml"))) {
            this::class.java.getResourceAsStream("/config/galaxy-proxy.toml")
                .copyTo(Files.newOutputStream(Paths.get("config", "galaxy-proxy.toml")))
        }

        this.config = Config { listOf(CoreSpec, GalaxySpec).forEach(::addSpec) }
            .from.toml.url(this::class.java.getResource("/config/galaxy-proxy.toml"))
            .from.toml.file("config/galaxy-proxy.toml")
            .from.env()
    }

    lateinit var manager: Manager

    @Inject
    fun init(proxy: ProxyServer, logger: Logger) {
        try {
            main = this
            this.proxy = proxy
            this.logger = logger

            this.kubernetesClient = KubernetesClient()
            this.redisClient = RedisClient()

            val factory = ConnectionFactory()
            factory.host = config[CoreSpec.rabbitMqHost]
            factory.port = config[CoreSpec.rabbitMqPort]
            factory.username = config[CoreSpec.rabbitMqUsername]
            factory.password = config[CoreSpec.rabbitMqPassword]
            factory.isAutomaticRecoveryEnabled = true
            factory.isTopologyRecoveryEnabled = true

            factory.exceptionHandler =
                object : DefaultExceptionHandler(),
                    CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
                    override fun handleTopologyRecoveryException(
                        conn: Connection?,
                        ch: Channel?,
                        exception: TopologyRecoveryException?
                    ) {
                        logger.error("Error while recovery", exception)
                    }
                }

            val connection = factory.newConnection()
            connection.addShutdownListener {
                logger.error("conn killed", it)
            }

            val channel = connection.createChannel()
            channel.addShutdownListener {
                logger.error("channel killed", it)
            }

            manager = Manager(channel, config[CoreSpec.rabbitMqExchange])
            manager.subscribe(MESSAGE_TOPIC)

            runBlocking {
                logger.info("Kubernetes Version: ${kubernetesClient.info().gitVersion}")
                logger.info("Redis version: ${redisClient.version()}")
            }

            proxy.channelRegistrar.register(ChatExchange.eventId)
            proxy.channelRegistrar.register(ChatExchange.eventIdResponse)
            logger.info("Galaxy Init!")
        } catch (err: Throwable) {
            logger.error("Failed to init the proxy!", err)
            exitProcess(1)
        }
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        try {
            proxy.commandManager.unregister("server") // Disable server command
            proxy.commandManager.register(Lobby(), "lobby")

            proxy.channelRegistrar.register(GalaxyPacket.MESSAGE_CHANNEL_ID)

            proxy.eventManager.register(this, PlayerListWatcher(config[CoreSpec.protocolVersion]))
            proxy.eventManager.register(this, TabListUpdater())
            proxy.eventManager.register(this, GalaxyPacket())

            // Start lobby TODO auto scale lobby
            GlobalScope.launch {
                try {
                    lobby = kubernetesClient.getOrCreateGalaxyAndVolume("galaxy-lobby", config[storageClass], "10Gi")
                        .let { if (!Readiness.isReady(it)) kubernetesClient.waitReady(it) else it }
                        .let {
                            proxy.registerServer(
                                ServerInfo(
                                    "galaxy-lobby",
                                    InetSocketAddress(it.status.podIP, 25565)
                                )
                            )
                        }
                } catch (e: Exception) {
                    exitProcess(1)
                }
            }

            // Connect player to lobby
            proxy.eventManager.register(this, ServerPreConnectEvent::class.java) {
                if (it.player.currentServer.isPresent || !this::lobby.isInitialized) return@register // Ignore exist player

                it.result = ServerPreConnectEvent.ServerResult.allowed(lobby)
            }

            chatExchange = ChatExchange(MESSAGE_TOPIC)
            proxy.eventManager.register(this, chatExchange)
        } catch (err: Throwable) {
            logger.error("Failed to init the proxy!", err)
            exitProcess(1)
        }
    }
}
