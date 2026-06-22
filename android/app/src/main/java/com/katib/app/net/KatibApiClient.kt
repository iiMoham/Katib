package com.katib.app.net

import com.katib.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Talks to the Katib proxy. The xAI key never lives in the app — only the proxy
 * base URL (and an optional shared secret) does, injected via BuildConfig.
 */
class KatibApiClient(
    private val baseUrl: String = BuildConfig.PROXY_BASE_URL,
    private val proxyKey: String = BuildConfig.PROXY_API_KEY,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    sealed interface Result {
        data class Ok(val response: CorrectResponse) : Result
        data class Error(val message: String, val code: Int = -1) : Result
    }

    suspend fun correct(text: String, mode: String, task: String): Result =
        withContext(Dispatchers.IO) {
            val payload = CorrectRequest(text = text, mode = mode, task = task)
            val body = json.encodeToString(CorrectRequest.serializer(), payload)
                .toRequestBody(mediaType)

            val builder = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/correct")
                .post(body)
            if (proxyKey.isNotBlank()) builder.addHeader("X-Katib-Key", proxyKey)

            try {
                client.newCall(builder.build()).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.Error("HTTP ${resp.code}", resp.code)
                    }
                    val parsed = json.decodeFromString(CorrectResponse.serializer(), raw)
                    Result.Ok(parsed)
                }
            } catch (e: Exception) {
                Result.Error(e.message ?: "network error")
            }
        }
}
