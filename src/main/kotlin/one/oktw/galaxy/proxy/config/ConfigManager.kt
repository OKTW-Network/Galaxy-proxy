package one.oktw.galaxy.proxy.config

import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.oktw.galaxy.proxy.config.model.GalaxySpec
import one.oktw.galaxy.proxy.config.model.ProxyConfig
import one.oktw.galaxy.proxy.config.model.RedisConfig
import one.oktw.galaxy.proxy.resourcepack.ResourcePack
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ConfigManager(private val basePath: Path = Paths.get("config")) {
    private val gson = Gson()

    lateinit var proxyConfig: ProxyConfig
        private set
    lateinit var redisConfig: RedisConfig
        private set
    val galaxies = HashMap<String, GalaxySpec>()
    val galaxiesResourpacePack = HashMap<String, ResourcePack>()

    init {
        readConfig()
        readGalaxies(FileSystems.newFileSystem(this::class.java.getResource("/config").toURI(), emptyMap<String, Any>()).getPath("/config/galaxies"))
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
                    GlobalScope.launch {
                        val resourcePack = galaxies[galaxyName]?.let { spec -> ResourcePack.new(spec.ResourcePack) } ?: return@launch
                        galaxiesResourpacePack[galaxyName] = resourcePack
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
            this::class.java.getResourceAsStream("/config/$name")
        }
    }
}
