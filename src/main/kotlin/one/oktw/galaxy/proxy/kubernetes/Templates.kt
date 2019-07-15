package one.oktw.galaxy.proxy.kubernetes

import com.fkorotkov.kubernetes.*
import com.velocitypowered.api.proxy.config.ProxyConfig
import io.fabric8.kubernetes.api.model.*
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.config.GalaxySpec
import java.nio.charset.StandardCharsets
import java.util.Arrays.asList

// Velocity config hack
private fun ProxyConfig.getForwardingSecret(): ByteArray {
    return this.javaClass.getMethod("getForwardingSecret").invoke(this) as ByteArray
}

object Templates {
    private val config = main.config
    private val JAVA_CLASS_CACHE = newVolume {
        this.name = "javasharedresources"; hostPath { path = "/tmp/javasharedresources"; type = "DirectoryOrCreate" }
    }
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

    fun galaxy(name: String, pvc: String): Pod {
        val mounts = asList(
            newVolumeMount { this.name = "javasharedresources";mountPath = "/app/javasharedresources" },
            newVolumeMount { this.name = "minecraft";subPath = "world";mountPath = "/app/minecraft/world" }
        )

        return newPod {
            metadata { this.name = name }
            spec {
                imagePullSecrets = asList(LocalObjectReference(config[GalaxySpec.pullSecret]))
                securityContext { fsGroup = 1000 }
                this.volumes = asList(
                    JAVA_CLASS_CACHE,
                    newVolume { this.name = "minecraft"; persistentVolumeClaim { claimName = pvc } }
                )

                initContainers = asList(newContainer {
                    this.name = "volume-mount-hack"
                    image = "busybox"
                    imagePullPolicy = "IfNotPresent"
                    command = asList(
                        "sh",
                        "-c",
                        "chown 1000 /app/javasharedresources /app/minecraft/world"
                    )
                    volumeMounts = mounts
                })

                containers = asList(newContainer {
                    this.name = "minecraft"
                    image = config[GalaxySpec.image]
                    lifecycle { preStop { exec { command = asList("control", "stop") } } }
                    ports = asList(newContainerPort {
                        this.name = "minecraft"
                        containerPort = 25565
                        protocol = "TCP"
                    })
                    readinessProbe {
                        initialDelaySeconds = 60
                        periodSeconds = 15
                        timeoutSeconds = 1
                        successThreshold = 1
                        exec { command = asList("control", "ping") }
                    }
                    livenessProbe {
                        initialDelaySeconds = 60
                        periodSeconds = 60
                        timeoutSeconds = 10
                        successThreshold = 1
                        failureThreshold = 3
                        exec { command = asList("control", "ping") }
                    }
                    volumeMounts = mounts
                    env = asList(EnvVar("FABRIC_PROXY_SECRET", forwardSecret, null))
                    resources {
                        requests = mapOf(
                            Pair("cpu", Quantity(config[GalaxySpec.Resource.cpuRequire])),
                            Pair("memory", Quantity(config[GalaxySpec.Resource.memoryRequire]))
                        )
                        limits = mapOf(
                            Pair("cpu", Quantity(config[GalaxySpec.Resource.cpuLimit])),
                            Pair("memory", Quantity(config[GalaxySpec.Resource.memoryLimit]))
                        )
                    }
                })
            }
        }
    }
//
//    private fun exec(vararg command: String) = V1ExecActionBuilder().withCommand(*command).build()
//
//    private fun PVC(name: String) = V1VolumeBuilder()
//        .withNewPersistentVolumeClaim()
//        .withClaimName(name)
//        .endPersistentVolumeClaim()
//        .build()
}
