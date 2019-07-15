package one.oktw.galaxy.proxy.api.packet

import one.oktw.galaxy.proxy.api.ProxyAPI.dummyUUID
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import java.util.*

@BsonDiscriminator
data class CreateGalaxy(val uuid: UUID = dummyUUID) : Packet {
    @BsonDiscriminator
    data class CreateProgress(val uuid: UUID = dummyUUID, val stage: ProgressStage = ProgressStage.Queue) : Packet
}
