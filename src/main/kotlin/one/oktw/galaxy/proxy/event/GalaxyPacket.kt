package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.ServerInfo
import io.fabric8.kubernetes.client.internal.readiness.Readiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.CreateGalaxy
import one.oktw.galaxy.proxy.api.packet.Packet
import one.oktw.galaxy.proxy.api.packet.ProgressStage
import one.oktw.galaxy.proxy.api.packet.WhoAmI
import one.oktw.galaxy.proxy.config.GalaxySpec.Storage
import java.net.InetSocketAddress
import java.util.*

class GalaxyPacket : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    companion object {
        val MESSAGE_CHANNEL_ID = MinecraftChannelIdentifier.create("galaxy", "proxy")
    }

    @Subscribe
    fun onPacket(event: PluginMessageEvent) {
        if (event.identifier == MESSAGE_CHANNEL_ID) event.result = ForwardResult.handled() else return

        val source = event.source as? ServerConnection ?: return
        val player = event.target as? Player ?: return

        launch {
            when (val data = ProxyAPI.decode<Packet>(event.data)) {
                is WhoAmI -> {
                    ProxyAPI.encode(WhoAmI.Result(UUID.fromString(source.server.serverInfo.name)))
                        .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                }
                is CreateGalaxy -> {
                    val kubernetes = main.kubernetesClient
                    val id = data.uuid.toString()
                    var galaxy = main.kubernetesClient.getGalaxy(id)

                    if (galaxy != null) {
                        if (!Readiness.isReady(galaxy)) {
                            // Send packet to server: Galaxy is starting
                            ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Starting))
                                .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }

                            galaxy = kubernetes.waitReady(galaxy)

                            // Send packet to server: Galaxy is starting
                            ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Started))
                                .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                        }
                    } else {
                        // Send packet to server: Galaxy is creating
                        ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Creating))
                            .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }

                        // Start galaxy
                        val config = main.config
                        galaxy = kubernetes.getOrCreateVolume(id, config[Storage.storageClass], config[Storage.size])
                            .let { main.kubernetesClient.createGalaxy(id, it) }

                        // Send packet to server: Galaxy is starting
                        ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Starting))
                            .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }

                        galaxy = kubernetes.waitReady(galaxy)

                        // Send packet to server: Galaxy is starting
                        ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Started))
                            .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                    }

                    // Send player to galaxy TODO only create not join
                    main.proxy.run {
                        getServer(id)
                            .orElseGet { registerServer(ServerInfo(id, InetSocketAddress(galaxy.status.podIP, 25565))) }
                            .let(player::createConnectionRequest).fireAndForget()
                    }
                }
            }
        }
    }
}
