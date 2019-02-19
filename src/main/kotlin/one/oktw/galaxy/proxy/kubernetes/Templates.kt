package one.oktw.galaxy.proxy.kubernetes

import io.kubernetes.client.models.V1Deployment
import io.kubernetes.client.models.V1DeploymentBuilder

object Templates {
    fun galaxyDeployment(name: String): V1Deployment = V1DeploymentBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build()
}