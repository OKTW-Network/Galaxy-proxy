package one.oktw.galaxy.proxy.kubernetes

import io.kubernetes.client.ApiException
import io.kubernetes.client.Configuration
import io.kubernetes.client.apis.AppsV1Api
import io.kubernetes.client.apis.CoreV1Api
import io.kubernetes.client.apis.VersionApi
import io.kubernetes.client.models.V1Deployment
import io.kubernetes.client.models.V1PersistentVolumeClaim
import io.kubernetes.client.models.VersionInfo
import io.kubernetes.client.util.Config
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.kubernetes.util.apiCallbackAdapter

class KubernetesClient {
    private val client = Config.defaultClient().also(Configuration::setDefaultApiClient)

    suspend fun info() = apiCallbackAdapter<VersionInfo> { VersionApi().getCodeAsync(it) }

    suspend fun createDeployment(namespace: String, deployment: V1Deployment) = apiCallbackAdapter<V1Deployment> {
        AppsV1Api().createNamespacedDeploymentAsync(namespace, deployment, null, null, null, it)
    }

    suspend fun getDeployment(namespace: String, name: String) = apiCallbackAdapter<V1Deployment> {
        AppsV1Api().readNamespacedDeploymentAsync(namespace, name, null, null, null, it)
    }

    suspend fun getOrCreateVolume(namespace: String, name: String) {
        try {
            getVolume(namespace, name)
        } catch (ex: ApiException) {
            main.logger.error("Failed to get Volume:", ex)
        }
    }

    suspend fun createVolume(namespace: String, volume: V1PersistentVolumeClaim): V1PersistentVolumeClaim {
        return apiCallbackAdapter {
            CoreV1Api().createNamespacedPersistentVolumeClaimAsync(namespace, volume, null, null, null, it)
        }
    }

    suspend fun getVolume(namespace: String, name: String) = apiCallbackAdapter<V1PersistentVolumeClaim> {
        CoreV1Api().readNamespacedPersistentVolumeClaimAsync(name, namespace, null, null, null, it)
    }
}