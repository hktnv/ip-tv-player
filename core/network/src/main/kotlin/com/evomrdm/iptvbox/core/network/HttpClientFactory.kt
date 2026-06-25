package com.evomrdm.iptvbox.core.network

import com.evomrdm.iptvbox.core.security.SecretRedactor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

object HttpClientFactory {
    fun create(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(45))
            .followRedirects(true)
            .addInterceptor(RedactingHeaderInterceptor())
            .build()
    }
}

class RedactingHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val redactedHeaders = SecretRedactor.redactHeaders(request.headers.toMultimap().mapValues {
            it.value.joinToString(";")
        })
        val safeRequest = request.newBuilder()
            .tag(SafeRequestLog::class.java, SafeRequestLog(SecretRedactor.redact(request.url.toString()), redactedHeaders))
            .build()
        return chain.proceed(safeRequest)
    }
}

data class SafeRequestLog(
    val url: String,
    val headers: Map<String, String>,
)

fun Request.safeLog(): SafeRequestLog {
    return tag(SafeRequestLog::class.java)
        ?: SafeRequestLog(SecretRedactor.redact(url.toString()), SecretRedactor.redactHeaders(headers.toMap()))
}
