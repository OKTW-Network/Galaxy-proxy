package one.oktw.galaxy.proxy.api.packet

import one.oktw.galaxy.proxy.api.ProxyAPI
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*

@BsonDiscriminator
class WhoAmI : Packet {
    @BsonDiscriminator
    class Result(val uuid: UUID = ProxyAPI.dummyUUID) : Packet
}
