package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.fabric8.kubernetes.client.readiness.Readiness
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import one.oktw.galaxy.proxy.command.Lobby
import one.oktw.galaxy.proxy.config.ConfigManager
import one.oktw.galaxy.proxy.event.ChatExchange
import one.oktw.galaxy.proxy.event.GalaxyPacket
import one.oktw.galaxy.proxy.event.PlayerListWatcher
import one.oktw.galaxy.proxy.event.TabListUpdater
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.pubsub.Manager
import one.oktw.galaxy.proxy.redis.RedisClient
import one.oktw.galaxy.proxy.resourcepack.ResourcePackManager
import org.slf4j.Logger
import java.net.InetSocketAddress
import kotlin.system.exitProcess

@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    companion object {
        private const val MESSAGE_TOPIC = "chat"

        lateinit var main: Main
            private set
    }

    lateinit var config: ConfigManager
        private set
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
    lateinit var manager: Manager
        private set
    lateinit var resourcePackManager: ResourcePackManager
        private set

    @Inject
    fun init(proxy: ProxyServer, logger: Logger) {
        try {
            main = this
            this.proxy = proxy
            this.logger = logger
            this.config = ConfigManager()
            this.kubernetesClient = KubernetesClient()
            this.redisClient = RedisClient()
            this.resourcePackManager = ResourcePackManager()

            manager = Manager(config.redisConfig.URI, config.redisConfig.PubSubPrefix)
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
            proxy.commandManager.unregister("shutdown") // Disable shutdown command
            proxy.commandManager.register(proxy.commandManager.metaBuilder("lobby").build(), Lobby())

            proxy.channelRegistrar.register(GalaxyPacket.MESSAGE_CHANNEL_ID)

            proxy.eventManager.register(this, PlayerListWatcher(config.proxyConfig.ProtocolVersion))
            proxy.eventManager.register(this, TabListUpdater())
            proxy.eventManager.register(this, GalaxyPacket())

            // Start lobby TODO auto-scale lobby
            launch {
                try {
                    lobby = kubernetesClient.getOrCreateGalaxyAndVolume("galaxy-lobby", config.galaxies["lobby"]!!)
                        .let { if (!Readiness.isPodReady(it)) kubernetesClient.waitReady(it) else it }
                        .let { proxy.registerServer(ServerInfo("galaxy-lobby", InetSocketAddress(it.status.podIP, 25565))) }
                } catch (e: Exception) {
                    logger.error("Failed create lobby.", e)
                    exitProcess(1)
                }
            }

            // Connect player to lobby
            proxy.eventManager.register(this, ServerPreConnectEvent::class.java) {
                if (it.player.currentServer.isPresent || !this::lobby.isInitialized) return@register // Ignore exist player

                it.result = ServerPreConnectEvent.ServerResult.allowed(lobby)
            }

            // Remove player on disconnect
            proxy.eventManager.register(this, DisconnectEvent::class.java) {
                this.resourcePackManager.removePlayer(it.player)
            }

            // Update resourcepacks
            @Suppress("UnstableApiUsage") proxy.eventManager.register(this, ServerPostConnectEvent::class.java) {
                // TODO: Get Galaxy Type
                if (it.player.currentServer.get().serverInfo.name == "galaxy-lobby") {
                    this.resourcePackManager.updatePlayerResourcePacks(it.player, "lobby")
                } else {
                    if (it.previousServer?.serverInfo?.name != "galaxy-lobby") return@register

                    this.resourcePackManager.updatePlayerResourcePacks(it.player, "normal_galaxy")
                }
            }

            // Connect back to lobby on disconnect from galaxies
            proxy.eventManager.register(this, KickedFromServerEvent::class.java) {
                if (it.server == lobby || !this::lobby.isInitialized || it.kickedDuringServerConnect()) {
                    it.result = KickedFromServerEvent.DisconnectPlayer.create(it.serverKickReason.orElse(Component.empty()))
                } else {
                    it.result = KickedFromServerEvent.RedirectPlayer.create(lobby, Component.empty())
                }
            }

            chatExchange = ChatExchange(MESSAGE_TOPIC)
            proxy.eventManager.register(this, chatExchange)
        } catch (err: Throwable) {
            logger.error("Failed to init the proxy!", err)
            exitProcess(1)
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        runBlocking { coroutineContext.job.cancelAndJoin() }
    }
}
