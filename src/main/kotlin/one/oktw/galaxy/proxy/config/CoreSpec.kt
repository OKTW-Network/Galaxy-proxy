package one.oktw.galaxy.proxy.config

import com.uchuhimo.konf.ConfigSpec

object CoreSpec : ConfigSpec() {
    val protocolVersion by required<Int>(description = "Minecraft protocol version")
    val redis by required<String>(description = "Redis connection URI")
    val kubernetes by optional<String?>(default = null, description = "Kubernetes API address")
}
