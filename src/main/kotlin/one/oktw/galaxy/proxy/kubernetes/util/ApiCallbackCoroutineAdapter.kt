package one.oktw.galaxy.proxy.kubernetes.util

import com.squareup.okhttp.Call
import io.kubernetes.client.ApiCallback
import io.kubernetes.client.ApiException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend inline fun <T> apiCallbackAdapter(crossinline callback: (ApiCallback<T>) -> Call): T {
    return suspendCancellableCoroutine {
        val call = callback(object : ApiCallback<T> {
            override fun onSuccess(result: T, p1: Int, p2: MutableMap<String, MutableList<String>>?) {
                it.resume(result)
            }

            override fun onFailure(exception: ApiException, p1: Int, p2: MutableMap<String, MutableList<String>>?) {
                it.resumeWith(Result.failure(exception))
            }

            override fun onUploadProgress(p0: Long, p1: Long, p2: Boolean) {}

            override fun onDownloadProgress(p0: Long, p1: Long, p2: Boolean) {}
        })

        it.invokeOnCancellation { call.cancel() }
    }
}
