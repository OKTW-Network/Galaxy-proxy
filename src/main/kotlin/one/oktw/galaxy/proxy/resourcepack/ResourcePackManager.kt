package one.oktw.galaxy.proxy.resourcepack

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.player.ResourcePackInfo
import one.oktw.galaxy.proxy.Main.Companion.main
import kotlin.math.min

class ResourcePackManager {
    private val appliedPacks: MutableMap<Player, List<ResourcePackInfo>> = mutableMapOf()

    fun updatePlayerResourcePacks(player: Player, galaxy: String) {
        val new = main.config.galaxies[galaxy]?.ResourcePacks?.mapNotNull { main.config.resourcePacks[it]?.packInfo() } ?: emptyList()
        val old = this.appliedPacks.getOrElse(player) { listOf() }

        // Matching new resource packs
        var updateIndex = old.lastIndex
        for (i in 0..min(old.size, new.size)) {
            if (old.getOrNull(i)?.hash != new.getOrNull(i)?.hash) {
                updateIndex = i
                break
            }
        }

        // Remove not match resource packs
        old.subList(updateIndex, old.size).forEach(player::removeResourcePacks)

        // Add new resource packs
        new.subList(updateIndex, new.size).forEach(player::sendResourcePacks)

        // Save player resource packs state
        appliedPacks[player] = new.toList()
    }

    // Remove player resource packs state
    fun removePlayer(player: Player) {
        appliedPacks.remove(player)
    }
}
