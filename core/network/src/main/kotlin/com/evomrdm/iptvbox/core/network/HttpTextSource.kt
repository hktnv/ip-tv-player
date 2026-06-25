package com.evomrdm.iptvbox.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource

class HttpTextSource(
    private val client: OkHttpClient = HttpClientFactory.create(),
) {
    suspend fun open(request: Request): BufferedSource = withContext(Dispatchers.IO) {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            error("HTTP ${response.code}: ${request.safeLog().url}")
        }
        response.body.source()
    }
}
