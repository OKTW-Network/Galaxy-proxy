package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.kyori.text.TextComponent
import one.oktw.galaxy.proxy.Main
import one.oktw.galaxy.proxy.redis.RedisClient
import java.util.*

class ChatExchange(val topic: String) {
    companion object {
        data class ChatData (
            val server: UUID,
            val player: UUID,
            val message: TextComponent
        )
    }

    @Subscribe
    fun onRelay (event: MessageDeliveryEvent) {
        if (event.topic != topic) return

        GlobalScope.launch {
            val players = Main.main.proxy.allPlayers

            if (event.data is ChatData) {
                val requiresSendTo = players.filter {
                    val name = it.currentServer.orElse(null)?.serverInfo?.name?: return@filter false
                    UUID.fromString(name) != event.data.server
                }

                requiresSendTo.forEach {
                    it.sendMessage(event.data.message)
                }
            }

        }
    }
}
