package one.oktw.galaxy.proxy.api.packet

import org.bson.codecs.pojo.annotations.BsonDiscriminator

@BsonDiscriminator
interface Packet {
    val type: PacketTypes
}
