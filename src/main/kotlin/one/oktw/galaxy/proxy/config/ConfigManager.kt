package one.oktw.galaxy.proxy.config

import com.google.gson.Gson
import one.oktw.galaxy.proxy.config.model.ProxyConfig
import one.oktw.galaxy.proxy.config.model.RedisConfig
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ConfigManager(private val basePath: Path = Paths.get("config")) {
    private val gson = Gson()

    lateinit var proxyConfig: ProxyConfig
        private set
    lateinit var redisConfig: RedisConfig
        private set

    init {
        readConfig()
    }

    // TODO reload command
    fun reloadAll() {
        reload()
    }

    fun reload() {
        readConfig()
    }


    private fun readConfig() {
        proxyConfig = fallbackToResource("proxy.json").reader().use { gson.fromJson(it, ProxyConfig::class.java) }
        redisConfig = fallbackToResource("redis.json").reader().use { gson.fromJson(it, RedisConfig::class.java) }
    }

    private fun fallbackToResource(name: String): InputStream {
        val file = basePath.resolve(name)

        return if (Files.isReadable(file)) {
            Files.newInputStream(file)
        } else {
            this::class.java.getResourceAsStream("/config/$name")!!
        }
    }
}
