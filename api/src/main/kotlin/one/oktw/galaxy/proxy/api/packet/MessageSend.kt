package one.oktw.galaxy.proxy.api.packet

import one.oktw.galaxy.proxy.api.ProxyAPI
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*
import java.util.Arrays.asList

// indicate which channel this user wish to listen.

@BsonDiscriminator
data class MessageSend(
    // default to uuid connection associated with
    val sender: UUID = ProxyAPI.dummyUUID,
    val message: String = "",
    val targets: List<UUID> = asList(),

    // message id, required if you need the response
    val id: UUID? = null,
    val requireCallback: Boolean = false
) : Packet {
    override val type = PacketTypes.MessageSend
}
