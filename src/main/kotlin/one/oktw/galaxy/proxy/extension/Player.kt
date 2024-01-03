package one.oktw.galaxy.proxy.extension

import com.velocitypowered.api.proxy.Player
import one.oktw.galaxy.proxy.Main.Companion.main

fun Player.sendPacket(o: Any) {
    try {
        val connection = this.javaClass.getMethod("getConnection").invoke(this)
        connection.javaClass.getMethod("write", Any::class.java).invoke(connection, o)
    } catch (e: Exception) {
        main.logger.error("Call velocity method error.", e)
    }
}
