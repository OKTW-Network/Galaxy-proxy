package one.oktw.galaxy.proxy.config

import com.uchuhimo.konf.ConfigSpec

object GalaxySpec : ConfigSpec() {
    val image by required<String>(description = "Galaxy docker image")
    val pullSecret by optional<String?>(null, description = "Kubernetes image pull secret")

    object Resource : ConfigSpec() {
        val cpuRequire by required<String>(description = "Container CPU require")
        val memoryRequire by required<String>(description = "Container memory require")
        val cpuLimit by required<String>(description = "Container CPU limit")
        val memoryLimit by required<String>(description = "Container memory limit")
    }

    object Storage : ConfigSpec() {
        val storageClass by required<String>(description = "Kubernetes storage class")
        val size by required<String>("Volume size")
    }
}
