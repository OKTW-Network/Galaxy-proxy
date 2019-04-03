package one.oktw.galaxy.proxy.model

import com.velocitypowered.api.proxy.server.ServerPing
import java.util.*

data class Galaxy(
    val uuid: UUID,
    var available: Boolean = false,
    val players: MutableList<ServerPing.SamplePlayer> = ArrayList()
)
