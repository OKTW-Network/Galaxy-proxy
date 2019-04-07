package one.oktw.galaxy.proxy.api.packet

import one.oktw.galaxy.proxy.api.ProxyAPI.dummyUUID
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*

@BsonDiscriminator
data class CreateGalaxy(val uuid: UUID = dummyUUID) : Packet {
    override val type = PacketTypes.CreateGalaxy
}
