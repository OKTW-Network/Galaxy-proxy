package one.oktw.galaxy.proxy.resourcepack

import com.velocitypowered.api.proxy.Player
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.extension.sendPacket

class ResourcePackHelper {
    companion object{
        fun trySendResourcePack(player: Player, galaxy: String){
            val resourcePack = main.config.galaxiesResourcePack[galaxy] ?: return
            // workaround Minecraft 1.20.3 not auto unload old resource pack
            try {
                val packet = Class.forName("com.velocitypowered.proxy.protocol.packet.RemoveResourcePackPacket").getConstructor().newInstance()
                player.sendPacket(packet)
            } catch (e: Exception) {
                main.logger.error("Send remove resource pack packet error.", e)
            }
            player.sendResourcePackOffer(
                main.proxy.createResourcePackBuilder(resourcePack.uri.toString())
                    .setHash(resourcePack.hash)
                    .setShouldForce(true)
                    .build()
            )
        }
    }
}
