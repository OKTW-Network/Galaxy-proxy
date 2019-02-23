package one.oktw.galaxy.proxy.extension

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerPing

fun Player.toSamplePlayer() = ServerPing.SamplePlayer(username, uniqueId)
