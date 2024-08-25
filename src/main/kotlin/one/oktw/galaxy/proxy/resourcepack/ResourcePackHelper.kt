package one.oktw.galaxy.proxy.resourcepack

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.player.ResourcePackInfo
import one.oktw.galaxy.proxy.Main.Companion.main
import kotlin.math.max

class ResourcePackHelper {
    companion object {
        fun updatePlayerResourcePacks(player: Player, galaxy: String) {
            val targetResourcePacks = main.config.galaxies[galaxy]?.ResourcePacks?.distinct()?.mapNotNull { main.config.resourcePacks[it]?.packInfo() } ?: return
            val appliedResourcePacks = player.appliedResourcePacks.toList().filterNotNull()

            var skipFurtherCheck = false
            val packsToQueue = mutableListOf<ResourcePackInfo>()
            val packsToRemove = mutableListOf<ResourcePackInfo>()

            for (index in 0..max(targetResourcePacks.size, appliedResourcePacks.size)) {
                if (targetResourcePacks.getOrNull(index)?.hash.contentEquals(appliedResourcePacks.getOrNull(index)?.hash) && !skipFurtherCheck) continue

                skipFurtherCheck = true
                if (index < appliedResourcePacks.size) packsToRemove.add(appliedResourcePacks[index])
                if (index < targetResourcePacks.size) packsToQueue.add(targetResourcePacks[index])
            }

            packsToRemove.forEach { pack -> player.removeResourcePacks(pack) }
            packsToQueue.forEach { pack -> player.sendResourcePacks(pack) }
        }
    }
}
