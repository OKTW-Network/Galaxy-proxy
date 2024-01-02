package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.MessageSend
import one.oktw.galaxy.proxy.api.packet.MessageSendResponse
import one.oktw.galaxy.proxy.api.packet.MessageUpdateChannel
import one.oktw.galaxy.proxy.api.packet.Packet
import one.oktw.galaxy.proxy.model.ChatData
import one.oktw.galaxy.proxy.model.ChatResponse
import java.util.*

class ChatExchange(private val topic: String) : CoroutineScope by main {
    companion object {
        private const val MESSAGE_TIMEOUT = 2000L

        val eventId = MinecraftChannelIdentifier.create("galaxy", "proxy-chat")!!
        val eventIdResponse = MinecraftChannelIdentifier.create("galaxy", "proxy-chat-response")!!
    }

    private val topicResponse = "$topic-response"
    private val listenMap = HashMap<UUID, List<UUID>>()
    private val ackMaps = HashMap<UUID, MessageSend>()

    @Subscribe
    fun onPlayerRegister(event: PluginMessageEvent) {
        if (event.identifier != eventId) return
        event.result = PluginMessageEvent.ForwardResult.handled()

        val source = event.source as? ServerConnection ?: return

        val packet = try {
            ProxyAPI.decode<Packet>(event.data) as? MessageUpdateChannel ?: return
        } catch (err: Throwable) {
            main.logger.error("Failed decode", err)
            return
        }

        val player = if (packet.user == ProxyAPI.dummyUUID) source.player.uniqueId else packet.user

        listenMap[player] = packet.listenTo
    }

    @Subscribe
    fun onServerSend(event: PluginMessageEvent) {
        if (event.identifier != eventId) return
        event.result = PluginMessageEvent.ForwardResult.handled()

        val source = event.source as? ServerConnection ?: return

        val unformattedPacket = try {
            ProxyAPI.decode<Packet>(event.data) as? MessageSend ?: return
        } catch (err: Throwable) {
            main.logger.error("Failed decode", err)
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
            launch {
                delay(MESSAGE_TIMEOUT)
                ackMaps.remove(packet.id!!)

                val players = main.proxy.allPlayers
                players.find { it.uniqueId == packet.sender }?.sendPluginMessage(
                    eventIdResponse, ProxyAPI.encode(
                        MessageSendResponse(
                            sender = packet.sender,
                            id = packet.id!!,
                            result = false
                        )
                    )
                )
            }
        }

        val chatData = ChatData(
            try {
                UUID.fromString(source.serverInfo.name)
            } catch (err: Throwable) {
                ProxyAPI.dummyUUID
            }, packet
        )

        main.manager.send(topic, chatData)
    }

    @Subscribe
    fun onRelay(event: MessageDeliveryEvent) {
        if (event.topic != topic) return

        if (event.data is ChatData) {
            val textComponent = GsonComponentSerializer.gson().deserialize(event.data.packet.message)

            main.proxy.allPlayers.forEach { player ->
                val playerSource = player.currentServer.orElse(null)?.let {
                    try {
                        UUID.fromString(it.serverInfo.name)
                    } catch (err: Throwable) {
                        null
                    }
                } ?: ProxyAPI.dummyUUID

                event.data.packet.targets.forEach { target ->
                    listenMap.computeIfAbsent(player.uniqueId) { listOf(player.uniqueId, ProxyAPI.globalChatChannel) }
                        .let {
                            if (target in it) {
                                if (event.data.server != playerSource && textComponent is TranslatableComponent) {
                                    val newText = Component.translatable().key(textComponent.key())
                                        .arguments(textComponent.arguments())
                                        .style(Style.style().color(NamedTextColor.GRAY).build())
                                        .append(textComponent.children())
                                        .build()

                                    player.sendMessage(newText)
                                } else {
                                    player.sendMessage(textComponent)
                                }
                            }
                        }
                }
            }
        }
    }

    @Subscribe
    fun onAck(event: MessageDeliveryEvent) {
        if (event.topic != topicResponse) return

        if (event.data is ChatResponse) {
            if (event.data.success && ackMaps.contains(event.data.id)) {
                val packet = ackMaps[event.data.id] ?: return
                ackMaps.remove(event.data.id)

                main.proxy.allPlayers.find { it.uniqueId == packet.sender }?.sendPluginMessage(
                    eventIdResponse,
                    ProxyAPI.encode(MessageSendResponse(sender = packet.sender, id = packet.id!!, result = true))
                )
            }
        }
    }
}
