package one.oktw.galaxy.proxy.model

import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.Packet
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*

@BsonDiscriminator
data class ChatResponse (
    val server: UUID = ProxyAPI.dummyUUID,
    val sender: UUID = ProxyAPI.dummyUUID,
    val id: UUID = ProxyAPI.dummyUUID,
    val success: Boolean = false
): Packet
