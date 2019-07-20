package one.oktw.galaxy.proxy.command

import com.velocitypowered.api.command.Command
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import one.oktw.galaxy.proxy.Main.Companion.main

class Lobby : Command {
    override fun execute(source: CommandSource, args: Array<out String>) {
        if (source !is Player) return

        source.createConnectionRequest(main.lobby).fireAndForget()
    }
}
