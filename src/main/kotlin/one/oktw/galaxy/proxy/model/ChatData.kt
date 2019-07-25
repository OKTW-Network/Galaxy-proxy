package one.oktw.galaxy.proxy.model

import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.MessageSend
import one.oktw.galaxy.proxy.api.packet.Packet
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*

@BsonDiscriminator
data class ChatData (
    val server: UUID = ProxyAPI.dummyUUID,
    // relay chat event packet
    val packet: MessageSend = MessageSend()
): Packet
