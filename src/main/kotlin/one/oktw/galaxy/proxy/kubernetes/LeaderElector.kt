package one.oktw.galaxy.proxy.kubernetes

import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfig
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock
import java.time.Duration

class LeaderElector(name: String, client: io.fabric8.kubernetes.client.KubernetesClient) {
    var isLeader = false
        private set

    private val future = client.leaderElector().withConfig(
        LeaderElectionConfig(
            LeaseLock(client.namespace, name, System.getenv("HOSTNAME")),
            Duration.ofSeconds(15),
            Duration.ofSeconds(10),
            Duration.ofSeconds(2),
            LeaderCallbacks({ isLeader = true }, { isLeader = false }, {}),
            false,
            name
        )
    ).build().start()

    fun stopElection() {
        future.cancel(true)
    }
}
