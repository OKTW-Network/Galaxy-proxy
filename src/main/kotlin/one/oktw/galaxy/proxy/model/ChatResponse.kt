package one.oktw.galaxy.proxy.model

import one.oktw.galaxy.proxy.api.packet.Packet
import java.util.*

data class ChatResponse (
    val server: UUID,
    val sender: UUID,
    val id: UUID,
    val success: Boolean = false
): Packet
