package one.oktw.galaxy.proxy.api.packet

import org.bson.codecs.pojo.annotations.BsonDiscriminator

@BsonDiscriminator
class SearchPlayer(val keyword: String = "", val number: Int = 10) : Packet {
    @BsonDiscriminator
    class Result(val players: List<String> = emptyList()) : Packet
}
