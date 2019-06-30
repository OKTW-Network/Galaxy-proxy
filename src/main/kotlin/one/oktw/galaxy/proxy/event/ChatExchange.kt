package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ServerConnection
import net.kyori.text.TextComponent
import net.kyori.text.serializer.ComponentSerializers
import one.oktw.galaxy.proxy.Main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.model.ChatData
import java.util.*

class ChatExchange(val topic: String) {
    val eventId = MinecraftChannelIdentifier.create("galaxy", "proxy/chat")

    @Subscribe
    fun onServerSend(event: PluginMessageEvent) {
        if (event.identifier.id != eventId) return
        val source = event.source as? ServerConnection ?: return
        val textComponentString = ProxyAPI.decode<String>(event.data)
        val textComponent = ComponentSerializers.JSON.deserialize(textComponentString) as? TextComponent ?: return
        val chatData = ChatData(UUID.fromString(source.serverInfo.name), textComponent)

        Main.main.manager.send(topic, chatData)
    }

    @Subscribe
    fun onRelay(event: MessageDeliveryEvent) {
        if (event.topic != topic) return

        val players = Main.main.proxy.allPlayers

        if (event.data is ChatData) {
            players.forEach {
                it.sendMessage(event.data.message)
            }
        }
    }

}
