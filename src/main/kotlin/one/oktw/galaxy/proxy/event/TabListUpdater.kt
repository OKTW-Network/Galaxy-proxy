package one.oktw.galaxy.proxy.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.player.TabListEntry
import com.velocitypowered.api.util.GameProfile
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import one.oktw.galaxy.proxy.Main.Companion.main
import java.util.concurrent.TimeUnit

class TabListUpdater : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    private val tabHeader = text("OKTW", NamedTextColor.DARK_PURPLE).append(text(" "))
        .append(text("Galaxy", NamedTextColor.YELLOW)).append(text(" "))
        .append(text("2.0", NamedTextColor.RED)).append(text(" "))
        .append(text("Lite", NamedTextColor.GREEN)).append(text(" "))
        .append(text("Beta", NamedTextColor.AQUA))
    private var tabFooter = empty()
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
        tabFooter = text("Online Player: ", NamedTextColor.BLUE)
            .append(text(main.redisClient.getPlayerNumber().toString(), NamedTextColor.GREEN))
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
