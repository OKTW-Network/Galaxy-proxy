/*
 * OKTW Galaxy Project
 * Copyright (C) 2018-2019
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package one.oktw.galaxy.proxy.resourcepack

import com.google.common.hash.Hashing
import com.velocitypowered.api.proxy.player.ResourcePackInfo
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import one.oktw.galaxy.proxy.Main.Companion.main
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI

class ResourcePack private constructor(url: String) {
    companion object {
        suspend fun new(url: String): ResourcePack = withContext(IO) { ResourcePack(url) }
    }

    var uri = URI(url)
        private set
    var hash: ByteArray
        private set

    init {
        this.hash = getHashFromUri(uri)
    }

    fun packInfo(): ResourcePackInfo = main.proxy.createResourcePackBuilder(this.uri.toString())
        .setHash(this.hash)
        .setShouldForce(true)
        .build()

    @Throws(FileNotFoundException::class)
    @Suppress("UnstableApiUsage", "DEPRECATION")
    private fun getHashFromUri(uri: URI): ByteArray {
        try {
            val hasher = Hashing.sha1().newHasher()
            uri.toURL().openStream().use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) {
                        break
                    }
                    hasher.putBytes(buf, 0, read)
                }
            }
            return hasher.hash().asBytes()
        } catch (e: IOException) {
            val ex = FileNotFoundException(e.toString())
            ex.initCause(e)
            throw ex
        }
    }
}
