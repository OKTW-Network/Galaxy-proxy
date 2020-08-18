package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.player.TabListEntry
import com.velocitypowered.api.util.GameProfile
import kotlinx.coroutines.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import one.oktw.galaxy.proxy.Main.Companion.main
import java.util.concurrent.TimeUnit

class TabListUpdater : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    private val tabHeader = TextComponent.builder()
        .append("OKTW", NamedTextColor.DARK_PURPLE).append(" ")
        .append("Galaxy", NamedTextColor.YELLOW).append(" ")
        .append("2.0", NamedTextColor.RED).append(" ")
        .append("Lite", NamedTextColor.GREEN).append(" ")
        .append("Beta", NamedTextColor.AQUA)
        .build()
    private var tabFooter: TextComponent = TextComponent.of("")
    private var playerListCache: List<Pair<GameProfile, Long>> = emptyList()
    private var updateTabList = true

    init {
        launch {
            while (updateTabList) {
                update()
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
    }

    @Subscribe
    fun playerJoin(event: PostLoginEvent) {
        launch {
            delay(300) // delay 300ms wait redis sync data
            update()
        }
    }

    @Subscribe
    fun playerSwitchServer(event: ServerConnectedEvent) {
        launch { update() }
    }

    @Subscribe
    fun onShutdown(event: ProxyShutdownEvent) {
        updateTabList = false
    }

    private suspend fun update() {
        tabFooter = TextComponent.builder()
            .append("Online Player: ", NamedTextColor.BLUE)
            .append(main.redisClient.getPlayerNumber().toString(), NamedTextColor.GREEN)
            .build()
        playerListCache = main.redisClient.getPlayers(number = 100).sortedBy { it.first.name }

        main.proxy.allPlayers.forEach { player ->
            player.tabList.setHeaderAndFooter(tabHeader, tabFooter)

            // Cleanup old data
            player.tabList.entries.forEach {
                if (it.profile.id != player.uniqueId) {
                    player.tabList.removeEntry(it.profile.id)
                }
            }

            playerListCache.forEach { (profile, ping) ->
                if (profile.id != player.uniqueId) {
                    TabListEntry.builder()
                        .gameMode(0) // TODO maybe need sync gameMode
                        .profile(profile)
                        .latency(ping.toInt())
                        .tabList(player.tabList)
                        .build()
                        .let(player.tabList::addEntry)
                }
            }
        }
    }
}
