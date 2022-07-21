package one.oktw.galaxy.proxy.kubernetes

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.VersionInfo
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import io.fabric8.kubernetes.client.okhttp.OkHttpClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.oktw.galaxy.proxy.Main.Companion.main
import one.oktw.galaxy.proxy.config.model.GalaxySpec
import one.oktw.galaxy.proxy.kubernetes.Templates.galaxy
import one.oktw.galaxy.proxy.kubernetes.Templates.volume
import java.util.concurrent.TimeUnit

class KubernetesClient {
    private val client = KubernetesClientBuilder().withHttpClientFactory(OkHttpClientFactory()).build() // TODO configurable api URL

    suspend fun info(): VersionInfo = withContext(IO) { client.kubernetesVersion }

    suspend fun getOrCreateGalaxyAndVolume(name: String, spec: GalaxySpec): Pod {
        return getGalaxy(name) ?: createGalaxy(name, spec)
    }

    fun createLeaderElector(name: String) = LeaderElector(name, client)

    suspend fun getGalaxy(name: String): Pod? = withContext(IO) {
        client.pods().withName(name).get()
    }

    suspend fun createGalaxy(name: String, spec: GalaxySpec): Pod = withContext(IO) {
        getOrCreateVolume(name, spec.Storage!!)
        client.resource(galaxy(name, spec)).createOrReplace()
    }

    suspend fun stopGalaxy(name: String) = withContext(IO) {
        val pod = client.pods().withName(name)
        pod.delete()
        pod.informOnCondition { it.filterNotNull().isEmpty() }.asDeferred().await()
    }

    suspend fun getOrCreateVolume(name: String, spec: GalaxySpec.GalaxyStorage): PersistentVolumeClaim {
        return getVolume(name) ?: createVolume(volume(name, spec.StorageClass, spec.Size))
    }

    suspend fun createVolume(volume: PersistentVolumeClaim): PersistentVolumeClaim = withContext(IO) {
        client.resource(volume).create()
    }

    suspend fun getVolume(name: String): PersistentVolumeClaim? = withContext(IO) {
        client.persistentVolumeClaims().withName(name).get()
    }

    suspend fun waitReady(pod: Pod): Pod = withContext(IO) {
        client.pods().withName(pod.metadata.name).waitUntilReady(5, TimeUnit.MINUTES)
    }

    suspend fun getGalaxiesConfig(watcher: (suspend CoroutineScope.(Map<String, String>) -> Unit)?) = withContext(IO) {
        val configMap = client.configMaps().withName("galaxies")

        if (watcher != null) {
            configMap.watch(object : Watcher<ConfigMap> {
                override fun eventReceived(action: Watcher.Action?, resource: ConfigMap) {
                    if (action == Watcher.Action.MODIFIED) runBlocking { watcher(resource.data) }
                }

                override fun onClose(cause: WatcherException?) {
                    main.logger.error("Watch config error", cause)
                }
            })
        }

        configMap.get().data
    }
}
