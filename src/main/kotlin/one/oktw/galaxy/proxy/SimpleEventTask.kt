package one.oktw.galaxy.proxy

import com.velocitypowered.api.event.Continuation
import com.velocitypowered.api.event.EventTask

class SimpleEventTask(val block: () -> Unit) : EventTask {
    override fun requiresAsync() = false

    override fun execute(continuation: Continuation) {
        try {
            block()
        } catch (e: Exception) {
            return continuation.resumeWithException(e)
        }
        continuation.resume()
    }
}
