package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import one.oktw.galaxy.proxy.Main
import one.oktw.galaxy.proxy.model.ChatData
import java.util.*

class ChatExchange(val topic: String) {

    @Subscribe
    fun onRelay (event: MessageDeliveryEvent) {
        if (event.topic != topic) return

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
