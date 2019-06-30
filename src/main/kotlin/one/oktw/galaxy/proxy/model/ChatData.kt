package one.oktw.galaxy.proxy.model

import one.oktw.galaxy.proxy.api.packet.MessageSend
import java.util.*

data class ChatData (
    val server: UUID,
    // relay chat event packet
    val packet: MessageSend
)