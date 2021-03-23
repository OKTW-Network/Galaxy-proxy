package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.ServerInfo
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException
import io.fabric8.kubernetes.client.internal.readiness.Readiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.*
import java.net.InetSocketAddress
import java.util.*

class GalaxyPacket : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    companion object {
        val MESSAGE_CHANNEL_ID: MinecraftChannelIdentifier = MinecraftChannelIdentifier.create("galaxy", "proxy")
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
                is SearchPlayer -> {
                    main.redisClient.getPlayers(data.keyword, data.number.toLong()).map { it.first.name }
                        .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, ProxyAPI.encode(SearchPlayer.Result(it))) }
                }
                is CreateGalaxy -> {
                    val kubernetes = main.kubernetesClient
                    val id = data.uuid.toString()
                    var galaxy = main.kubernetesClient.getGalaxy(id)

                    if (galaxy != null) {
                        if (!Readiness.isPodReady(galaxy)) {
                            // Send packet to server: Galaxy is starting
                            ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Starting))
                                .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }

                            galaxy = try {
                                kubernetes.waitReady(galaxy)
                            } catch (e: KubernetesClientTimeoutException) {
                                main.logger.error("Waiting exist galaxy ({}) ready timeout!", data.uuid, e)
                                ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Failed))
                                    .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                                return@launch
                            }

                            // Send packet to server: Galaxy is started
                            ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Started))
                                .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                        }
                    } else {
                        // Send packet to server: Galaxy is creating
                        ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Creating))
                            .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }

                        // Start galaxy
                        galaxy = kubernetes.getOrCreateGalaxyAndVolume(id, main.config.galaxies["normal_galaxy"]!!) // TODO multi type

                        // Send packet to server: Galaxy is starting
                        ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Starting))
                            .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }

                        galaxy = try {
                            kubernetes.waitReady(galaxy)
                        } catch (e: KubernetesClientTimeoutException) {
                            main.logger.error("Waiting new galaxy ({}) ready timeout!", data.uuid, e)
                            ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Failed))
                                .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                            return@launch
                        }

                        // Send packet to server: Galaxy is started
                        ProxyAPI.encode(CreateGalaxy.CreateProgress(data.uuid, ProgressStage.Started))
                            .let { source.sendPluginMessage(MESSAGE_CHANNEL_ID, it) }
                    }

                    // Send player to galaxy TODO only create not join
                    main.proxy.run {
                        val address = InetSocketAddress(galaxy!!.status.podIP, 25565)
                        var server = getServer(id).orElseGet { registerServer(ServerInfo(id, address)) }

                        if (server.serverInfo.address != address) {
                            unregisterServer(server.serverInfo)
                            server = registerServer(ServerInfo(id, address))
                        }

                        player.createConnectionRequest(server).fireAndForget()
                    }
                }
            }
        }
    }
}
