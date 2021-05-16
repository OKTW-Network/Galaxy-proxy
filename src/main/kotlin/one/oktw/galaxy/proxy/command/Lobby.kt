package one.oktw.galaxy.proxy.command

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.connection.Player
import one.oktw.galaxy.proxy.Main.Companion.main

class Lobby : SimpleCommand { // TODO migrate to BrigadierCommand
    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        if (source !is Player) return

        source.createConnectionRequest(main.lobby).fireAndForget()
    }
}
