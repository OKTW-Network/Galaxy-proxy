package one.oktw.galaxy.proxy.resourcepack

import com.velocitypowered.api.proxy.Player
import one.oktw.galaxy.proxy.Main.Companion.main

class ResourcePackHelper {
    companion object{
        fun trySendResourcePack(player: Player, galaxy: String){
            val resourcePack = main.config.galaxiesResourpacePack[galaxy] ?: return
            player.sendResourcePackOffer(
                main.proxy.createResourcePackBuilder(resourcePack.uri.toString())
                    .setHash(resourcePack.hash)
                    .setShouldForce(true)
                    .build()
            )
        }
    }
}
