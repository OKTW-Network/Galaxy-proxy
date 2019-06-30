package one.oktw.galaxy.proxy.api.packet

import one.oktw.galaxy.proxy.api.ProxyAPI
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*
import java.util.Arrays.asList

// indicate which channel this user wish to listen.
@BsonDiscriminator
data class MessageUpdateChannel(
    // default to uuid connection associated with
    val user: UUID = ProxyAPI.dummyUUID,
    val listenTo: List<UUID> = asList()
) : Packet {
    override val type = PacketTypes.MessageUpdateChannel
}
