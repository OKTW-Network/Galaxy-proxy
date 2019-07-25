package one.oktw.galaxy.proxy.pubsub.data

import one.oktw.galaxy.proxy.api.ProxyAPI
import one.oktw.galaxy.proxy.api.packet.Packet
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*

@BsonDiscriminator
data class  MessageWrapper (
    val source: UUID = ProxyAPI.dummyUUID,
    val message: Packet = Packet.dummy
)
