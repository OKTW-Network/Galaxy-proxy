package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerPing
import kotlinx.coroutines.*
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.extension.toSamplePlayer
import java.util.concurrent.TimeUnit

class PlayerListWatcher(private val protocolVersion: Int) : CoroutineScope {
    private val job = Job()
    private var updatePlayer = true
    override val coroutineContext
        get() = Dispatchers.IO + job

    init {
        launch {
            while (updatePlayer) {
                delay(TimeUnit.MINUTES.toMillis(3))
                forceUpdatePlayers()
            }
        }
    }

    @Subscribe
    fun onPlayerJoin(event: PostLoginEvent) {
        launch { main.redisClient.addPlayer(event.player.toSamplePlayer()) }
    }

    @Subscribe
    fun onPlayerDisconnect(event: DisconnectEvent) {
        launch { main.redisClient.delPlayer(event.player.toSamplePlayer()) }
    }

    @Subscribe
    fun onShutdown(event: ProxyShutdownEvent) {
        updatePlayer = false
        runBlocking { job.cancelAndJoin() }
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        runBlocking {
            val number = async { main.redisClient.getPlayerNumber().toInt() }
            val players = async { main.redisClient.getPlayers() }

            event.ping = event.ping.asBuilder()
                .onlinePlayers(number.await())
                .maximumPlayers(Int.MIN_VALUE)
                .samplePlayers(*players.await().toTypedArray())
                .version(ServerPing.Version(protocolVersion, "OKTW Galaxy"))
                .build()
        }
    }

    private fun forceUpdatePlayers() {
        main.proxy.allPlayers
            .map(Player::toSamplePlayer)
            .let { launch { main.redisClient.addPlayers(it) } }
    }
}
