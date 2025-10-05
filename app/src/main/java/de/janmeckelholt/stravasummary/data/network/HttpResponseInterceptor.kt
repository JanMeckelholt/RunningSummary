package de.janmeckelholt.stravasummary.data.network

import de.janmeckelholt.stravasummary.utils.ordinal
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
class HttpResponseInterceptor(
    private val handleExpectedFailures: (response: Response, request: Request) -> okhttp3.Request? = { _, request -> request }
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originReq = chain.request()
        var unhandledError: Boolean? = null
        var retryCount = 0
        val maxRetryCount = 3
        Timber.i("___intercepting... ${originReq.url}")
        var response = chain.proceed(chain.request())
        Timber.i("___response code: ${response.code} - ${response.peekBody(10000).string()}")
        while (!response.isSuccessful && retryCount < maxRetryCount) {
            retryCount++
            var newRequest = handleExpectedFailures(response, originReq)
            if (newRequest == null) {
                unhandledError = true
                newRequest = originReq
            }
            Timber.w("HttpResponseInterceptor: Error ${response.code} - ${response.peekBody(10000).string()}")
            response.close()
            Timber.w("retrying request: $newRequest - ${retryCount.ordinal()} retry")
            response = chain.proceed(newRequest)

        }
        if ((!response.isSuccessful && retryCount >= maxRetryCount) || unhandledError == true) {
            Timber.e("Request ${originReq.url} failed. Retry count: $retryCount - ${unhandledError?.let { "unhandled Error" }}")
        }
        if  (response.body == null) {
            Timber.w("Response body is null for request: ${originReq.url}")
            return response
        }
        return response
    }
}