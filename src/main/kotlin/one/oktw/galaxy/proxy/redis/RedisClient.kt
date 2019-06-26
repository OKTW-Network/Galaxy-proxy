package one.oktw.galaxy.proxy.redis

import com.velocitypowered.api.proxy.server.ServerPing
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import kotlinx.coroutines.future.await
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.config.CoreSpec
import one.oktw.galaxy.proxy.model.Galaxy
import java.util.*

class RedisClient {
    companion object {
        private const val DB_PLAYERS = 0
        private const val DB_GALAXY = 1
    }

    private val client = RedisClient.create(main.config[CoreSpec.redis]).connect()

    suspend fun version() = client.async()
        .info("server")
        .await()
        .split("\r\n")
        .first { it.startsWith("redis_version") }

    // Player
    suspend fun addPlayers(players: List<ServerPing.SamplePlayer>, ttl: Long = 300) {
        players.forEach { addPlayer(it, ttl) }
    }

    suspend fun addPlayer(player: ServerPing.SamplePlayer, ttl: Long = 300) {
        client.async()
            .apply {
                select(DB_PLAYERS)
                set(player.name, player.id.toString()).await()
                expire(player.name, ttl).await()
            }
    }

    suspend fun delPlayer(player: ServerPing.SamplePlayer) {
        client.async()
            .apply { select(DB_PLAYERS) }
            .del(player.name)
            .await()
    }

    suspend fun getPlayerNumber(): Long = client.async()
        .apply { select(DB_PLAYERS) }
        .dbsize()
        .await()

    suspend fun getPlayers(limit: Long = 12) = client.async()
        .run {
            select(DB_PLAYERS)

            scan(ScanArgs().limit(limit))
                .await()
                .keys
                .run {
                    if (isEmpty()) {
                        emptyList()
                    } else {
                        mget(*toTypedArray())
                            .await()
                            .map { ServerPing.SamplePlayer(it.key, UUID.fromString(it.value)) }
                    }
                }
        }

    // Galaxy
    suspend fun addGalaxy(galaxy: Galaxy) {
        client.async()
            .run {
                select(DB_GALAXY)
            }
    }

    suspend fun getGalaxies(): List<UUID> = client.async()
        .run {
            select(DB_GALAXY)
            keys("*").await()
                .map { UUID.fromString(it) }
        }

    suspend fun getGalaxyPlayers(galaxy: Galaxy) {
        client.async()
            .run {
                select(DB_GALAXY)
            }
    }
}
