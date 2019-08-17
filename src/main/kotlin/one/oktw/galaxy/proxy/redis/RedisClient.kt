package one.oktw.galaxy.proxy.redis

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.util.GameProfile
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import kotlinx.coroutines.future.await
import one.oktw.galaxy.proxy.Main.Companion.main
import java.util.*

class RedisClient {
    companion object {
        private const val DB_PLAYERS = 0
        private const val DB_GALAXY = 1
    }

    private val gson = Gson()
    private val client = RedisClient.create(main.config.redisConfig.URI).connect()

    suspend fun version() = client.async()
        .info("server")
        .await()
        .split("\r\n")
        .first { it.startsWith("redis_version") }

    // Player
    suspend fun addPlayers(players: List<Player>, ttl: Long = 180) {
        players.forEach { addPlayer(it, ttl) }
    }

    suspend fun addPlayer(player: Player, ttl: Long = 180) {
        client.async()
            .apply {
                select(DB_PLAYERS)
                if (exists(player.username).await() == 0L) {
                    hmset(
                        player.username,
                        mapOf(
                            Pair("uuid", player.uniqueId.toString()),
                            Pair("latency", player.ping.toString()),
                            Pair("properties", gson.toJson(player.gameProfileProperties))
                        )
                    ).await()
                }

                hset(player.username, "latency", player.ping.toString()).await()
                expire(player.username, ttl).await()
            }
    }

    suspend fun delPlayer(name: String) {
        client.async()
            .apply { select(DB_PLAYERS) }
            .del(name)
            .await()
    }

    suspend fun getPlayerNumber(): Long = client.async()
        .apply { select(DB_PLAYERS) }
        .dbsize()
        .await()

    suspend fun getPlayers(keyword: String = "", number: Long = 12) = client.async().run {
        select(DB_PLAYERS)

        scan(ScanArgs().limit(number).match("$keyword*"))
            .await()
            .keys
            .run {
                if (isEmpty()) {
                    emptyList()
                } else {
                    map {
                        val data = hgetall(it).await()

                        GameProfile(
                            UUID.fromString(data["uuid"]),
                            it,
                            gson.fromJson(
                                data["properties"],
                                object : TypeToken<List<GameProfile.Property>>() {}.type
                            )
                        ) to data["latency"]!!.toLong()
                    }
                }
            }
    }
}
