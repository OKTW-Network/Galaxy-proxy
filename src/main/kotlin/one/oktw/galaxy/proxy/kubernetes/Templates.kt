package one.oktw.galaxy.proxy.kubernetes

import com.fkorotkov.kubernetes.*
import io.fabric8.kubernetes.api.model.*
import java.util.Arrays.asList

object Templates {
    private val JAVA_CLASS_CACHE = newVolume {
        this.name = "javasharedresources"; hostPath { path = "/tmp/javasharedresources"; type = "DirectoryOrCreate" }
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
            newVolumeMount { this.name = "minecraft";subPath = "world";mountPath = "/app/minecraft/world" },
            newVolumeMount {
                this.name = "minecraft";subPath = "sponge-config";mountPath = "/app/minecraft/config/sponge/worlds"
            }
        )

        return newPod {
            metadata { this.name = name }
            spec {
                imagePullSecrets = asList(LocalObjectReference("gitlab")) //TODO config
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
                        "chown 1000 /app/javasharedresources /app/minecraft/world  /app/minecraft/config/sponge/worlds"
                    )
                    volumeMounts = mounts
                })

                containers = asList(newContainer {
                    this.name = "minecraft"
                    image = "registry.gitlab.com/oktw-network/docker-galaxy:v0.3.1"
                    lifecycle { preStop { exec { command = asList("control", "stop") } } }
                    ports = asList(newContainerPort {
                        this.name = "minecraft"
                        containerPort = 25565
                        protocol = "TCP"
                    })
                    readinessProbe { tcpSocket { port = IntOrString(25565) } }
                    livenessProbe {
                        initialDelaySeconds = 60
                        periodSeconds = 60
                        timeoutSeconds = 10
                        successThreshold = 1
                        failureThreshold = 3
                        exec { command = asList("control", "ping") }
                    }
                    volumeMounts = mounts
                    resources {
                        //TODO config
                        requests = mapOf(
                            Pair("cpu", Quantity("300m")),
                            Pair("memory", Quantity("700Mi"))
                        )
                        limits = mapOf(
                            Pair("cpu", Quantity("2000m")),
                            Pair("memory", Quantity("2Gi"))
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
