package one.oktw.galaxy.proxy.config.model

data class GalaxySpec(
    val Image: String,
    val PullSecret: String,
    val Type: GalaxyType,
    // List of resource pack keys (defined with resource_packs.json) to apply in order (first item applies first).
    // In Minecraft version prior to 1.20.2, only the last resource pack will be applied.
    val ResourcePacks: List<String>,
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
