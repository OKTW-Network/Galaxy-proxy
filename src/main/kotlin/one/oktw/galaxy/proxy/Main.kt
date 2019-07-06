package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.uchuhimo.konf.Config
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.fabric8.kubernetes.client.internal.readiness.Readiness
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.oktw.galaxy.proxy.config.CoreSpec
import one.oktw.galaxy.proxy.config.GalaxySpec
import one.oktw.galaxy.proxy.config.GalaxySpec.Storage.storageClass
import one.oktw.galaxy.proxy.event.GalaxyPacket
import one.oktw.galaxy.proxy.event.PlayerListWatcher
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.redis.RedisClient
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Paths

@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main {
    companion object {
        lateinit var main: Main
            private set
    }

    val config: Config

    private lateinit var lobby: RegisteredServer

    lateinit var kubernetesClient: KubernetesClient
        private set
    lateinit var redisClient: RedisClient
        private set
    lateinit var proxy: ProxyServer
        private set
    lateinit var logger: Logger
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

    @Inject
    fun init(proxy: ProxyServer, logger: Logger) {
        main = this
        this.proxy = proxy
        this.logger = logger

        this.kubernetesClient = KubernetesClient()
        this.redisClient = RedisClient()

        runBlocking {
            logger.info("Kubernetes Version: ${kubernetesClient.info().gitVersion}")
            logger.info("Redis version: ${redisClient.version()}")
        }

        logger.info("Galaxy Init!")
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        proxy.channelRegistrar.register(GalaxyPacket.MESSAGE_CHANNEL_ID)

        proxy.eventManager.register(this, PlayerListWatcher(config[CoreSpec.protocolVersion]))
        proxy.eventManager.register(this, GalaxyPacket())

        // Start lobby TODO auto scale lobby
        GlobalScope.launch {
            lobby = kubernetesClient.getOrCreateGalaxyAndVolume("galaxy-lobby", config[storageClass], "10Gi")
                .let { if (!Readiness.isReady(it)) kubernetesClient.waitReady(it) else it }
                .let { proxy.registerServer(ServerInfo("galaxy-lobby", InetSocketAddress(it.status.podIP, 25565))) }
        }

        // Connect player to lobby
        proxy.eventManager.register(this, ServerPreConnectEvent::class.java) {
            if (it.player.currentServer.isPresent || !this::lobby.isInitialized) return@register // Ignore exist player

            it.result = ServerPreConnectEvent.ServerResult.allowed(lobby)
        }
    }
}
