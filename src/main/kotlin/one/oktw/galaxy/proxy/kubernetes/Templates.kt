package one.oktw.galaxy.proxy.kubernetes

import com.fkorotkov.kubernetes.*
import com.velocitypowered.api.proxy.config.ProxyConfig
import io.fabric8.kubernetes.api.model.*
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.config.model.GalaxySpec
import java.nio.charset.StandardCharsets
import java.util.Arrays.asList

// Velocity config hack
private fun ProxyConfig.getForwardingSecret(): ByteArray {
    return this.javaClass.getMethod("getForwardingSecret").invoke(this) as ByteArray
}

object Templates {
    private val forwardSecret by lazy {
        main.proxy.configuration.getForwardingSecret().toString(StandardCharsets.UTF_8)
    }

    fun volume(name: String, storageClass: String, size: String): PersistentVolumeClaim = newPersistentVolumeClaim {
        metadata {
            this.name = name
        }

        spec {
            storageClassName = storageClass
            accessModes = asList("ReadWriteOnce")

            resources {
                this.requests = mapOf(Pair("storage", Quantity(size)))
            }
        }
    }

    fun galaxy(name: String, spec: GalaxySpec): Pod {
        if (spec.Storage == null) throw IllegalArgumentException("Storage spec undefined!")

        return newPod {
            metadata { this.name = name }
            spec {
                imagePullSecrets = asList(LocalObjectReference(spec.PullSecret))
                securityContext { fsGroup = 1000 }
                this.volumes = asList(newVolume { this.name = "minecraft"; persistentVolumeClaim { claimName = name } })

                containers = asList(newContainer {
                    this.name = "minecraft"
                    image = spec.Image
                    env = asList(
                        EnvVar("FABRIC_PROXY_SECRET", forwardSecret, null),
                        EnvVar("resourcePack", spec.ResourcePack, null)
                    )

                    ports = asList(newContainerPort {
                        this.name = "minecraft"
                        containerPort = 25565
                        protocol = "TCP"
                    })

                    volumeMounts = asList(
                        newVolumeMount { this.name = "minecraft";subPath = "world";mountPath = "/app/minecraft/world" }
                    )

                    lifecycle { preStop { exec { command = asList("control", "stop") } } }
                    readinessProbe {
                        initialDelaySeconds = 30
                        periodSeconds = 15
                        timeoutSeconds = 1
                        successThreshold = 1
                        exec { command = asList("control", "ping") }
                    }
                    livenessProbe {
                        initialDelaySeconds = 30
                        periodSeconds = 60
                        timeoutSeconds = 10
                        successThreshold = 1
                        failureThreshold = 3
                        exec { command = asList("control", "ping") }
                    }

                    resources {
                        requests = mapOf(
                            Pair("cpu", Quantity(spec.Resource.CPURequest)),
                            Pair("memory", Quantity(spec.Resource.MemoryRequest))
                        )
                        limits = mapOf(
                            Pair("cpu", Quantity(spec.Resource.CPULimit)),
                            Pair("memory", Quantity(spec.Resource.MemoryLimit))
                        )
                    }
                })
            }
        }
    }
}
