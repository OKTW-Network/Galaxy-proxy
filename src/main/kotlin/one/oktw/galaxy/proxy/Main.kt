package one.oktw.galaxy.proxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.oktw.galaxy.proxy.kubernetes.KubernetesClient
import one.oktw.galaxy.proxy.redis.RedisClient
import org.slf4j.Logger
import java.util.*

@Plugin(id = "galaxy-proxy", name = "Galaxy proxy side plugin", version = "1.0-SNAPSHOT")
class Main {
    companion object {
        lateinit var main: Main
            private set
    }

    private val kubernetesClient by lazy { KubernetesClient() }
    private val redisClient = RedisClient()
    lateinit var proxy: ProxyServer
    lateinit var logger: Logger

    @Inject
    fun init(proxy: ProxyServer, logger: Logger) {
        main = this
        this.proxy = proxy
        this.logger = logger

        GlobalScope.launch {
            logger.info("Kubernetes Version: ${kubernetesClient.info()}")
            logger.info("Redis version: ${redisClient.version()}")
        }

        logger.info("Galaxy Init!")
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        // TODO
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        event.ping = event.ping.asBuilder()
            .onlinePlayers(Int.MAX_VALUE)
            .maximumPlayers(Int.MIN_VALUE)
            .samplePlayers(*getSamplePlayer())
            .build()
    }

    private fun getSamplePlayer(): Array<SamplePlayer> {
        return arrayOf(SamplePlayer("dummy", UUID.randomUUID()))
    }
}
