package one.oktw.galaxy.proxy.config

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.config.model.GalaxySpec
import one.oktw.galaxy.proxy.config.model.ProxyConfig
import one.oktw.galaxy.proxy.config.model.RedisConfig
import one.oktw.galaxy.proxy.resourcepack.ResourcePack
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class ConfigManager(private val basePath: Path = Paths.get("config")) {
    private val gson = Gson()

    lateinit var proxyConfig: ProxyConfig
        private set
    lateinit var redisConfig: RedisConfig
        private set
    val galaxies = HashMap<String, GalaxySpec>()
    val galaxiesResourcePack = ConcurrentHashMap<String, ResourcePack>()

    init {
        readConfig()
        readGalaxies(FileSystems.newFileSystem(this::class.java.getResource("/config")!!.toURI(), emptyMap<String, Any>()).getPath("/config/galaxies"))
        readGalaxies(basePath.resolve("galaxies"))
    }

    fun reloadAll() {
        reload()
        reloadGalaxies()
    }

    fun reload() {
        readConfig()
    }

    fun reloadGalaxies() {
        readGalaxies(basePath.resolve("galaxies"))
    }

    private fun readConfig() {
        proxyConfig = fallbackToResource("proxy.json").reader().use { gson.fromJson(it, ProxyConfig::class.java) }
        redisConfig = fallbackToResource("redis.json").reader().use { gson.fromJson(it, RedisConfig::class.java) }
    }

    private fun readGalaxies(path: Path) {
        Files.newDirectoryStream(path).use {
            it.forEach { file ->
                if (Files.isDirectory(file) || !Files.isReadable(file)) return@forEach

                Files.newBufferedReader(file).use { json ->
                    val galaxyName = file.fileName.toString().substringBeforeLast(".")
                    galaxies[galaxyName] = gson.fromJson(json, GalaxySpec::class.java)
                    runBlocking {
                        try {
                            galaxiesResourcePack[galaxyName] = galaxies[galaxyName]?.let { spec -> if (spec.ResourcePack.isNotBlank()) ResourcePack.new(spec.ResourcePack) else null } ?: return@runBlocking
                        } catch (e: Exception) {
                            main.logger.error("Resource pack load failed!", e)
                        }
                    }
                }
            }
        }
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
