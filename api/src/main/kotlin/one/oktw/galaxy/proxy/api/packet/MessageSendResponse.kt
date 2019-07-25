package one.oktw.galaxy.proxy.api.packet

import one.oktw.galaxy.proxy.api.ProxyAPI
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*
import java.util.Arrays.asList

// indicate which channel this user wish to listen.

@BsonDiscriminator
data class MessageSendResponse(
    // default to uuid connection associated with
    val sender: UUID = ProxyAPI.dummyUUID,

    // message id
    val id: UUID = ProxyAPI.dummyUUID,
    // result for this operation
    val result: Boolean = true
) : Packet {
}
