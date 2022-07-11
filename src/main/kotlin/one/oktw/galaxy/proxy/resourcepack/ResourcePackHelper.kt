package one.oktw.galaxy.proxy.resourcepack

import com.google.common.hash.Hashing
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

class ResourcePackHelper {
    companion object {
        @Throws(FileNotFoundException::class)
        @Suppress("UnstableApiUsage", "DEPRECATION")
        fun getHashFromUrl(url: String): ByteArray {
            try {
                val hasher = Hashing.sha1().newHasher()
                URL(url).openStream().use { input ->
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
}
