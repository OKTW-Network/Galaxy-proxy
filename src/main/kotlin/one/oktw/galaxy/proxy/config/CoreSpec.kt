package one.oktw.galaxy.proxy.config

import com.uchuhimo.konf.ConfigSpec

object CoreSpec : ConfigSpec() {
    val protocolVersion by required<Int>(description = "Minecraft protocol version")
    val redis by required<String>(description = "Redis connection URI")
    val kubernetes by optional<String?>(default = null, description = "Kubernetes API address")
    val rabbitMqHost by required<String>(description = "rabbitMq URI")
    val rabbitMqPort by required<Int>(description = "rabbitMq Port")
    val rabbitMqUsername by required<String>(description = "rabbitMq username")
    val rabbitMqPassword by required<String>(description = "rabbitMq password")
    val rabbitMqExchange by required<String>(description = "rabbitMq exchange")
}
