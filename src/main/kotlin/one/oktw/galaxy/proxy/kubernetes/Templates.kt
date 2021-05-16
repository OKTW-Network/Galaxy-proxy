package one.oktw.galaxy.proxy.kubernetes

import com.fkorotkov.kubernetes.*
import com.velocitypowered.api.proxy.config.ProxyConfig
import io.fabric8.kubernetes.api.model.*
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.config.model.GalaxySpec
import java.nio.charset.StandardCharsets

// Velocity config hack
private fun ProxyConfig.getForwardingSecret(): ByteArray {
    return this.javaClass.getMethod("getForwardingSecret").invoke(this) as ByteArray
}

object Templates {
    private val forwardSecret by lazy {
        main.proxy.configuration().getForwardingSecret().toString(StandardCharsets.UTF_8)
    }

    fun volume(name: String, storageClass: String, size: String): PersistentVolumeClaim = newPersistentVolumeClaim {
        metadata {
            this.name = name
        }

        spec {
            storageClassName = storageClass
            accessModes = listOf("ReadWriteOnce")

            resources {
                this.requests = mapOf(Pair("storage", Quantity(size)))
            }
        }
    }

    fun galaxy(name: String, spec: GalaxySpec): Pod {
        requireNotNull(spec.Storage) { "Storage spec undefined!" }

        return newPod {
            metadata { this.name = name }
            spec {
                imagePullSecrets = listOf(LocalObjectReference(spec.PullSecret))
                securityContext { fsGroup = 1000 }
                this.volumes = listOf(newVolume {
                    this.name = "minecraft"
                    persistentVolumeClaim { claimName = name }
                })

                containers = listOf(newContainer {
                    this.name = "minecraft"
                    image = spec.Image
                    imagePullPolicy = "Always"
                    env = listOf(
                        EnvVar("FABRIC_PROXY_SECRET", forwardSecret, null),
                        EnvVar("resourcePack", spec.ResourcePack, null),
                        EnvVar("GALAXY_ID", name, null)
                    )

                    ports = listOf(newContainerPort {
                        this.name = "minecraft"
                        containerPort = 25565
                        protocol = "TCP"
                    })

                    volumeMounts = listOf(
                        newVolumeMount {
                            this.name = "minecraft"
                            subPath = "world"
                            mountPath = "/app/minecraft/world"
                        }
                    )

                    lifecycle { preStop { exec { command = listOf("control", "stop") } } }
                    terminationGracePeriodSeconds = 60
                    readinessProbe {
                        initialDelaySeconds = 30
                        periodSeconds = 5
                        timeoutSeconds = 3
                        exec { command = listOf("control", "ping") }
                    }
                    livenessProbe {
                        initialDelaySeconds = 180
                        periodSeconds = 30
                        timeoutSeconds = 10
                        successThreshold = 1
                        failureThreshold = 3
                        exec { command = listOf("control", "ping") }
                    }

                    if (spec.Resource != null) {
                        resources {
                            requests = listOfNotNull(
                                spec.Resource.CPURequest?.let { Pair("cpu", Quantity(it)) },
                                spec.Resource.MemoryRequest?.let { Pair("memory", Quantity(it)) }
                            ).toMap()
                            limits = listOfNotNull(
                                spec.Resource.CPULimit?.let { Pair("cpu", Quantity(it)) },
                                spec.Resource.MemoryLimit?.let { Pair("memory", Quantity(it)) }
                            ).toMap()
                        }
                    }
                })
            }
        }
    }
}
