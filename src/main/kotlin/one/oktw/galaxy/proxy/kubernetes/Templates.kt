package one.oktw.galaxy.proxy.kubernetes

import io.kubernetes.client.models.V1Deployment
import io.kubernetes.client.models.V1DeploymentBuilder
import io.kubernetes.client.models.V1PersistentVolumeClaim
import io.kubernetes.client.models.V1PersistentVolumeClaimBuilder

object Templates {
    fun galaxyDeployment(name: String): V1Deployment = V1DeploymentBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build()

    fun volume(name: String, storageClass: String): V1PersistentVolumeClaim = V1PersistentVolumeClaimBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .build()
}
