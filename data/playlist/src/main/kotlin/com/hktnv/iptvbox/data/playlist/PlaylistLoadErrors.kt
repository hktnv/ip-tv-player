package com.hktnv.iptvbox.data.playlist

import com.hktnv.iptvbox.core.security.SecretRedactor
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun playlistLoadError(url: String, throwable: Throwable): IllegalStateException {
    return IllegalStateException(playlistLoadUserMessage(url, throwable), rootCause(throwable))
}

internal fun playlistLoadUserMessage(url: String, throwable: Throwable): String {
    val root = rootCause(throwable)
    val detail = safeMessage(root)
    return when {
        looksTruncatedUrl(url) ->
            "Adres eksik görünüyor. Oynatma listesi URL'sini tam olarak girin."

        root is MalformedURLException || detail.contains("unexpected url", ignoreCase = true) ->
            "Adres biçimi hatalı. URL http:// veya https:// ile başlamalı ve tam yazılmalı."

        root is UnknownHostException || detail.contains("Unable to resolve host", ignoreCase = true) ->
            "Sunucu adresi bulunamadı. URL'yi ve internet bağlantısını kontrol edin."

        root is SocketTimeoutException || detail.contains("timeout", ignoreCase = true) ->
            "Sunucu geç yanıt verdi. Bağlantınızı kontrol edip tekrar deneyin."

        root.isLikelyTlsForPlainHttp() ->
            "Bağlantı kurulamadı. Sunucu HTTPS desteklemiyor olabilir; adresi kontrol edin."

        detail.contains("HTTP 401") || detail.contains("HTTP 403") ->
            "Sunucu erişimi reddetti. Kullanıcı adı, parola veya liste yetkisini kontrol edin."

        detail.contains("HTTP 404") ->
            "Adres bulunamadı. Oynatma listesi URL'si yanlış veya artık geçerli değil."

        detail.contains("HTTP 5") ->
            "Sunucu şu anda listeyi veremiyor. Biraz sonra tekrar deneyin."

        detail.contains("Failed to connect", ignoreCase = true) ||
            detail.contains("connection refused", ignoreCase = true) ->
            "Sunucuya bağlanılamadı. Adres, port veya internet bağlantısını kontrol edin."

        detail.contains("İçerik bulunamadı", ignoreCase = true) ->
            detail

        else ->
            "Liste yüklenemedi. Adresi ve bağlantınızı kontrol edip tekrar deneyin."
    }
}

internal fun safeMessage(throwable: Throwable): String {
    return SecretRedactor.redact(throwable.message ?: throwable.cause?.message ?: "Bilinmeyen hata")
}

private fun rootCause(throwable: Throwable): Throwable {
    return if (throwable is TimedFetchException) throwable.cause ?: throwable else throwable
}

private fun Throwable.isLikelyTlsForPlainHttp(): Boolean {
    val text = (message ?: cause?.message).orEmpty().lowercase()
    return text.contains("tls") ||
        text.contains("ssl") ||
        text.contains("wrong version number") ||
        text.contains("not an ssl") ||
        text.contains("packet header")
}

private fun looksTruncatedUrl(url: String): Boolean {
    val trimmed = url.trim()
    return trimmed.endsWith("=") ||
        trimmed.endsWith("?") ||
        trimmed.endsWith("&") ||
        trimmed.contains("?username=", ignoreCase = true) && !trimmed.contains("&password=", ignoreCase = true)
}
