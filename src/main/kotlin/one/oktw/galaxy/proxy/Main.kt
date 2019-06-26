package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.uchuhimo.konf.Config
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.runBlocking
import one.oktw.galaxy.proxy.config.CoreSpec
import one.oktw.galaxy.proxy.config.GalaxySpec
import one.oktw.galaxy.proxy.event.PlayerListWatcher
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.redis.RedisClient
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.Paths

@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main {
    companion object {
        lateinit var main: Main
            private set
    }

    val config: Config

    private lateinit var playerListWatcher: PlayerListWatcher

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
        playerListWatcher = PlayerListWatcher(config[CoreSpec.protocolVersion])

        proxy.eventManager.register(this, playerListWatcher)
    }
}
