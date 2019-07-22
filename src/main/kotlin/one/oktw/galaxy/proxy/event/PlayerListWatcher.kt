package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.server.ServerPing
import kotlinx.coroutines.*
import one.oktw.galaxy.proxy.Main.Companion.main
import java.util.concurrent.TimeUnit

class PlayerListWatcher(private val protocolVersion: Int) : CoroutineScope {
    private val job = SupervisorJob()
    private var updatePlayer = true
    override val coroutineContext by lazy { Dispatchers.Unconfined + job }

    init {
        launch {
            while (updatePlayer) {
                delay(TimeUnit.MINUTES.toMillis(1))
                forceUpdatePlayers()
            }
        }
    }

    @Subscribe
    fun onPlayerJoin(event: PostLoginEvent) {
        launch { main.redisClient.addPlayer(event.player) }
    }

    @Subscribe
    fun onPlayerDisconnect(event: DisconnectEvent) {
        launch { main.redisClient.delPlayer(event.player.username) }
    }

    @Subscribe
    fun onShutdown(event: ProxyShutdownEvent) {
        updatePlayer = false

        runBlocking {
            main.proxy.allPlayers.forEach { main.redisClient.delPlayer(it.username) }

            job.cancelAndJoin()
        }
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        runBlocking {
            val number = async { main.redisClient.getPlayerNumber().toInt() }
            val players = async { main.redisClient.getPlayers(number = 12) }

            event.ping = event.ping.asBuilder()
                .onlinePlayers(number.await())
                .maximumPlayers(Int.MIN_VALUE)
                .samplePlayers(*players.await()
                    .map { (profile, _) -> ServerPing.SamplePlayer(profile.name, profile.id) }
                    .toTypedArray()
                )
                .version(ServerPing.Version(protocolVersion, "OKTW Galaxy"))
                .build()
        }
    }

    private fun forceUpdatePlayers() {
        launch { main.redisClient.addPlayers(main.proxy.allPlayers.toMutableList()) }
    }
}
