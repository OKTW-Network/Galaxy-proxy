package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.oktw.galaxy.proxy.event.PlayersWatcher
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.redis.RedisClient
import org.slf4j.Logger

@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main {
    companion object {
        lateinit var main: Main
            private set
    }

    private val kubernetesClient = KubernetesClient()
    private val redisClient = RedisClient()
    private lateinit var playersWatcher: PlayersWatcher
    lateinit var proxy: ProxyServer
    lateinit var logger: Logger

    @Inject
    fun init(proxy: ProxyServer, logger: Logger) {
        main = this
        this.proxy = proxy
        this.logger = logger

        GlobalScope.launch {
            logger.info("Kubernetes Version: ${kubernetesClient.info().gitVersion}")
            logger.info("Redis version: ${redisClient.version()}")
        }

        logger.info("Galaxy Init!")
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        playersWatcher = PlayersWatcher(proxy, redisClient)

        proxy.eventManager.register(this, playersWatcher)
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        event.connection.protocolVersion

        val players = runBlocking { redisClient.getPlayers() }

        event.ping = event.ping.asBuilder()
            .onlinePlayers(players.size)
            .maximumPlayers(Int.MIN_VALUE)
            .samplePlayers(*players.toTypedArray())
            .version(ServerPing.Version(340, "OKTW Galaxy"))
            .build()
    }
}
