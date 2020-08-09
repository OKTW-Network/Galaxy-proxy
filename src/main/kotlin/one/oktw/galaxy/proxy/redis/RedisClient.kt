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
    private val client = RedisClient.create(main.config.redisConfig.URI)
    private val playersDB = client.connect().async().apply { select(DB_PLAYERS) }
    private val galaxyDB = client.connect().async().apply { select(DB_GALAXY) }

    fun version() = client.connect().sync()
        .info("server")
        .split("\r\n")
        .first { it.startsWith("redis_version") }

    // Player
    suspend fun addPlayers(players: List<Player>, ttl: Long = 180) {
        players.forEach { addPlayer(it, ttl) }
    }

    suspend fun addPlayer(player: Player, ttl: Long = 180) {
        playersDB.apply {
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

    suspend fun delPlayer(name: String) = playersDB.del(name).await()

    suspend fun getPlayerNumber(): Long = playersDB.dbsize().await()

    suspend fun getPlayers(keyword: String = "", number: Long = 12) = playersDB.run {
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
