package one.oktw.galaxy.proxy.config

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    val resourcePacks = ConcurrentHashMap<String, ResourcePack>()

    init {
        readConfig()
        readResourcePacks()
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
        readResourcePacks()
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
                    galaxies[file.fileName.toString().substringBeforeLast(".")] = gson.fromJson(json, GalaxySpec::class.java)
                }
            }
        }
    }

    private fun readResourcePacks() {
        val mapType = object : TypeToken<Map<String, String>>() {}
        val packs = fallbackToResource("resource_packs.json").reader().use { gson.fromJson(it, mapType) }
        packs.forEach { pack ->
            runBlocking {
                try {
                    if (pack.value.isNotEmpty()) {
                        resourcePacks[pack.key] = ResourcePack.new(pack.value)
                    }
                } catch (e: Exception) {
                    main.logger.error("Resource pack {} load failed!", pack.key, e)
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
