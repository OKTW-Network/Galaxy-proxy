package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.text.TextComponent
import net.kyori.text.serializer.gson.GsonComponentSerializer
import one.oktw.galaxy.proxy.Main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.MessageSend
import one.oktw.galaxy.proxy.api.packet.MessageSendResponse
import one.oktw.galaxy.proxy.api.packet.MessageUpdateChannel
import one.oktw.galaxy.proxy.model.ChatData
import one.oktw.galaxy.proxy.model.ChatResponse
import java.util.*
import kotlin.collections.HashMap

class ChatExchange(val topic: String) {
    companion object {
        val eventId: MinecraftChannelIdentifier = MinecraftChannelIdentifier.create("galaxy", "proxy-chat")
        val eventIdResponse: MinecraftChannelIdentifier = MinecraftChannelIdentifier.create("galaxy", "proxy/chat-response")
    }

    private val MESSAGE_TIMEOUT = 2000L
    private val topicResponse = "$topic-response"
    private val listenMap = HashMap<UUID, List<UUID>>()
    private val ackMaps = HashMap<UUID, MessageSend>()

    @Subscribe
    fun onPlayerRegister(event: PluginMessageEvent) {
        if (!event.identifier.equals(eventId)) return
        val source = event.source as? ServerConnection ?: return

        val packet = try {
            ProxyAPI.decode<MessageUpdateChannel>(event.data)
        } catch (err: Throwable) {
            return
        }

        val player = if (packet.user == ProxyAPI.dummyUUID) {
            source.player.uniqueId
        } else {
            packet.user
        }

        listenMap[player] = packet.listenTo
    }

    @Subscribe
    fun onServerSend(event: PluginMessageEvent) {
        if (!event.identifier.equals(eventId)) return
        val source = event.source as? ServerConnection ?: return

        val unformattedPacket = try {
            ProxyAPI.decode<MessageSend>(event.data)
        } catch (err: Throwable) {
            return
        }

        val packet = if (unformattedPacket.sender == ProxyAPI.dummyUUID) {
            MessageSend(
                sender = source.player.uniqueId,
                message = unformattedPacket.message,
                targets = unformattedPacket.targets,
                id = unformattedPacket.id,
                requireCallback = unformattedPacket.requireCallback
            )
        } else {
            unformattedPacket
        }

        if (packet.requireCallback && packet.id != null) {
            ackMaps[packet.id as UUID] = packet
            GlobalScope.launch {
                delay(MESSAGE_TIMEOUT)
                ackMaps.remove(packet.id!!)

                val players = Main.main.proxy.allPlayers
                players.find { it.uniqueId == packet.sender }?.sendPluginMessage(
                    eventIdResponse, ProxyAPI.encode(
                        MessageSendResponse(
                            sender = packet.sender,
                            id = packet.id!!,
                            result = true
                        )
                    )
                )
            }
        }

        val chatData = ChatData(UUID.fromString(source.serverInfo.name), packet)

        Main.main.manager.send(topic, chatData)

    }

    @Subscribe
    fun onRelay(event: MessageDeliveryEvent) {
        if (event.topic != topic) return

        val players = Main.main.proxy.allPlayers

        if (event.data is ChatData) {
            val textComponent =
                GsonComponentSerializer.INSTANCE.deserialize(event.data.packet.message) as? TextComponent ?: return

            players.forEach {player ->
                event.data.packet.targets.forEach {target ->
                    if (target in listenMap.computeIfAbsent(player.uniqueId) { listOf(player.uniqueId, ProxyAPI.globalChatChannel) }) {
                        player.sendMessage(textComponent)
                    }
                }
            }
        }
    }

    @Subscribe
    fun onAck(event: MessageDeliveryEvent) {
        if (event.topic != topicResponse) return

        val players = Main.main.proxy.allPlayers

        if (event.data is ChatResponse) {
            if (event.data.success && ackMaps.contains(event.data.id)) {
                val packet = ackMaps[event.data.id] ?: return
                ackMaps.remove(event.data.id)

                players.find { it.uniqueId == packet.sender }?.sendPluginMessage(
                    eventIdResponse, ProxyAPI.encode(
                        MessageSendResponse(
                            sender = packet.sender,
                            id = packet.id!!,
                            result = true
                        )
                    )
                )
            }
        }
    }
}
