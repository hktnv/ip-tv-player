package com.hktnv.iptvbox.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

private const val DEFAULT_USER_AGENT = "IPTVBoxPersonal/AndroidTV"

private val sharedPlayerHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectionPool(ConnectionPool(6, 5, TimeUnit.MINUTES))
        .build()
}

internal fun cleanPlaybackHeaders(headers: Map<String, String>): Map<String, String> {
    return headers
        .filterKeys { it.isNotBlank() }
        .filterValues { it.isNotBlank() }
}

@UnstableApi
internal fun createIptvDataSourceFactory(
    context: Context,
    headers: Map<String, String>,
): DataSource.Factory {
    val cleanHeaders = cleanPlaybackHeaders(headers)
    val httpFactory = OkHttpDataSource.Factory(sharedPlayerHttpClient)
        .setUserAgent(cleanHeaders["User-Agent"] ?: DEFAULT_USER_AGENT)
        .setDefaultRequestProperties(cleanHeaders)
    return DefaultDataSource.Factory(context, httpFactory)
}
