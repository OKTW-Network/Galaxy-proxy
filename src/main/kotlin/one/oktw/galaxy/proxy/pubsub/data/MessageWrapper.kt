package one.oktw.galaxy.proxy.pubsub.data

import one.oktw.galaxy.proxy.api.packet.Packet
import java.util.*

data class MessageWrapper (
    val source: UUID,
    val message: Packet
)
