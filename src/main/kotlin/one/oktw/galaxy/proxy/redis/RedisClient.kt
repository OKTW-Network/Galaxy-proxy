package one.oktw.galaxy.proxy.redis

import com.velocitypowered.api.proxy.server.ServerPing
import io.lettuce.core.RedisClient
import kotlinx.coroutines.future.await
import java.util.*

class RedisClient {
    companion object {
        private const val KEY_PLAYERS = "players"
    }

    // TODO move connect string to config
    private val client = RedisClient.create("redis-sentinel://redis-ha#mymaster").connect()

    suspend fun version() = client.async()
        .info("server")
        .await()
        .split("\r\n")
        .first { it.startsWith("redis_version") }

    suspend fun addPlayers(players: List<ServerPing.SamplePlayer>, ttl: Long = 300) {
        players.forEach { addPlayer(it, ttl) }
    }

    suspend fun addPlayer(player: ServerPing.SamplePlayer, ttl: Long = 300) {
        client.async()
            .apply {
                set("$KEY_PLAYERS:${player.name}", player.id.toString()).await()
                expire("$KEY_PLAYERS:${player.name}", ttl).await()
            }
    }

    suspend fun delPlayer(player: ServerPing.SamplePlayer) {
        client.async()
            .del("$KEY_PLAYERS:${player.name}")
            .await()
    }

    suspend fun getPlayers() = client.async()
        .keys("$KEY_PLAYERS:*")
        .await()
        .run {
            if (isEmpty()) return@run emptyList<ServerPing.SamplePlayer>()

            client.async()
                .mget(*toTypedArray())
                .await()
                .map { ServerPing.SamplePlayer(it.key.drop(KEY_PLAYERS.length + 1), UUID.fromString(it.value)) }
        }
}
