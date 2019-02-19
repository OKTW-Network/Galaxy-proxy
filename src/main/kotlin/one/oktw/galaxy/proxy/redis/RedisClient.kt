package one.oktw.galaxy.proxy.redis

import io.lettuce.core.RedisClient
import kotlinx.coroutines.future.await

class RedisClient {
    private val client = RedisClient.create("redis-sentinel://redis-ha#mymaster").connect()

    suspend fun version() = client.async()
        .info("server")
        .await()
        .split("\r\n")
        .first { it.startsWith("redis_version") }
}