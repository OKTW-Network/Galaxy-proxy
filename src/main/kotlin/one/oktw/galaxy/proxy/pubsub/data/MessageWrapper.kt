package one.oktw.galaxy.proxy.pubsub.data

import java.util.*

data class MessageWrapper (
    val source: UUID,
    val message: Any
)
