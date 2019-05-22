package one.oktw.galaxy.proxy.model

import net.kyori.text.TextComponent
import java.util.*

data class ChatData (
    val server: UUID,
    val player: UUID,
    val message: TextComponent
)
