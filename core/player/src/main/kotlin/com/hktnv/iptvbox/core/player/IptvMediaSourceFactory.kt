package com.hktnv.iptvbox.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory

internal fun iptvTsExtractorFlags(): Int {
    return DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
}

@UnstableApi
internal fun createIptvExtractorsFactory(): ExtractorsFactory {
    return DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setTsExtractorFlags(iptvTsExtractorFlags())
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
