package one.oktw.galaxy.proxy.kubernetes

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.VersionInfo
import io.fabric8.kubernetes.client.internal.readiness.ReadinessWatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import one.oktw.galaxy.proxy.kubernetes.Templates.galaxy
import one.oktw.galaxy.proxy.kubernetes.Templates.volume
import java.util.concurrent.TimeUnit

class KubernetesClient {
    private val client = DefaultKubernetesClient() // TODO configurable api URL

    suspend fun info(): VersionInfo = withContext(IO) { client.version }

    suspend fun getOrCreateGalaxyAndVolume(name: String, storageClass: String, size: String = "1Gi"): Pod {
        return getGalaxy(name) ?: createGalaxy(name, getOrCreateVolume(name, storageClass, size))
    }

    suspend fun getGalaxy(name: String): Pod? = withContext(IO) {
        client.pods().withName(name).get()
    }

    suspend fun createGalaxy(name: String, pvc: PersistentVolumeClaim): Pod = withContext(IO) {
        client.pods().create(galaxy(name, pvc.metadata.name))
    }

    suspend fun getOrCreateVolume(name: String, storageClass: String, size: String = "1Gi"): PersistentVolumeClaim {
        return getVolume(name) ?: createVolume(volume(name, storageClass, size))
    }

    suspend fun createVolume(volume: PersistentVolumeClaim): PersistentVolumeClaim = withContext(IO) {
        client.persistentVolumeClaims().create(volume)
    }

    suspend fun getVolume(name: String): PersistentVolumeClaim? = withContext(IO) {
        client.persistentVolumeClaims().withName(name).get()
    }

    suspend fun waitReady(pod: Pod) {
        withContext(IO) {
            ReadinessWatcher(pod).also { client.pods().withName(pod.metadata.name).watch(it) }
                .await(10, TimeUnit.MINUTES)
        }
    }
}
