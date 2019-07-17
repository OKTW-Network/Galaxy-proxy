package one.oktw.galaxy.proxy.api

import org.bson.BsonBinaryReader
import org.bson.BsonBinaryWriter
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import org.bson.codecs.pojo.Conventions.*
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.io.BasicOutputBuffer
import java.nio.ByteBuffer
import java.util.*

object ProxyAPI {
    private val DEFAULT_ENCODER_CONTEXT = EncoderContext.builder().build()
    val dummyUUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val globalChatChannel = UUID.fromString("00000000-0000-0000-0000-000000000001")

    val codecRegistries: CodecRegistry = CodecRegistries.fromProviders(
        ValueCodecProvider(),
        BsonValueCodecProvider(),
        DocumentCodecProvider(),
        IterableCodecProvider(),
        MapCodecProvider(),
        Jsr310CodecProvider(),
        PojoCodecProvider.builder()
            .conventions(listOf(SET_PRIVATE_FIELDS_CONVENTION, ANNOTATION_CONVENTION, CLASS_AND_PROPERTY_CONVENTION))
            .automatic(true)
            .build()
    )

    fun encode(obj: Any): ByteArray {
        val buffer = BasicOutputBuffer()

        codecRegistries.get(obj.javaClass).encode(BsonBinaryWriter(buffer), obj, DEFAULT_ENCODER_CONTEXT)

        return buffer.toByteArray()
    }

    inline fun <reified T> decode(byte: ByteArray): T = decode(ByteBuffer.wrap(byte))

    inline fun <reified T> decode(buffer: ByteBuffer): T {
        return codecRegistries.get(T::class.java).decode(BsonBinaryReader(buffer), DecoderContext.builder().build())
    }
}
