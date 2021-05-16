package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Event
import one.oktw.galaxy.proxy.api.packet.Packet

class MessageDeliveryEvent(
    val topic: String,
    val data: Packet
) : Event
