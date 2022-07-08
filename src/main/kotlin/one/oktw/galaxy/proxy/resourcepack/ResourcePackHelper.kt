package one.oktw.galaxy.proxy.resourcepack

import com.velocitypowered.api.proxy.Player
import one.oktw.galaxy.proxy.Main.Companion.main

class ResourcePackHelper {
    companion object{
        fun trySendResourcePack(player: Player, galaxy: String){
            val lobbyResourcePack = main.config.galaxiesResourpacePack[galaxy] ?: return
            player.sendResourcePackOffer(
                main.proxy.createResourcePackBuilder(lobbyResourcePack.uri.toString())
                    .setHash(lobbyResourcePack.hash)
                    .setShouldForce(true)
                    .build()
            )
        }
    }
}
