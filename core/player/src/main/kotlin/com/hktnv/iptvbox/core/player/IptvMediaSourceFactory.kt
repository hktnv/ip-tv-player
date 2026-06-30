package com.hktnv.iptvbox.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor

internal const val IPTV_TS_TIMESTAMP_SEARCH_BYTES = 64 * 1024

internal fun iptvTsExtractorFlags(): Int {
    return DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
        DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM
}

@UnstableApi
internal fun createIptvExtractorsFactory(): ExtractorsFactory {
    return DefaultExtractorsFactory()
        .setConstantBitrateSeekingAlwaysEnabled(true)
        .setTsExtractorMode(TsExtractor.MODE_SINGLE_PMT)
        .setTsExtractorFlags(iptvTsExtractorFlags())
        .setTsExtractorTimestampSearchBytes(IPTV_TS_TIMESTAMP_SEARCH_BYTES)
}

@UnstableApi
internal fun createIptvMediaSourceFactory(
    context: Context,
    headers: Map<String, String>,
): DefaultMediaSourceFactory {
    return DefaultMediaSourceFactory(
        createIptvDataSourceFactory(context, headers),
        createIptvExtractorsFactory(),
    )
}
