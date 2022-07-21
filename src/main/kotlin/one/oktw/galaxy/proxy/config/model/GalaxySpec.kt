package one.oktw.galaxy.proxy.config.model

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.player.ResourcePackInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.oktw.galaxy.proxy.resourcepack.ResourcePackHelper

data class GalaxySpec(
    val Image: String,
    val PullSecret: String,
    val Type: GalaxyType,
    val ResourcePack: String,
    val Resource: GalaxyResource? = null,
    val Storage: GalaxyStorage? = null
) {
    private var resourcePackHash: ByteArray? = null

    init {
        GlobalScope.launch {
            resourcePackHash = ResourcePackHelper.getHashFromUrl(ResourcePack)
        }
    }

    data class GalaxyResource(
        val CPULimit: String?,
        val CPURequest: String?,
        val MemoryLimit: String?,
        val MemoryRequest: String?
    )

    data class GalaxyStorage(
        val StorageClass: String,
        val Size: String
    )

    fun getResourcePack(proxy: ProxyServer): ResourcePackInfo {
        return proxy.createResourcePackBuilder(ResourcePack)
            .also { if (resourcePackHash != null) it.setHash(resourcePackHash) }
            .build()
    }
}
