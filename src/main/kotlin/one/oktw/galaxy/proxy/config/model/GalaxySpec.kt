package one.oktw.galaxy.proxy.config.model

data class GalaxySpec(
    val Image: String,
    val PullSecret: String,
    val Type: GalaxyType,
    val ResourcePack: String,
    val Resource: GalaxyResource? = null,
    val Storage: GalaxyStorage? = null
) {
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
}
