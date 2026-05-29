package com.zhuimeng.dreambox.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object GithubChartApi {

    private const val TAG = "GithubChartApi"
    private const val BASE_URL = "https://ghchart.rshah.org"
    private const val TIMEOUT_SECONDS = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSvgBytes(color: String, username: String): ByteArray {
        val url = "$BASE_URL/$color/$username"
        Log.d(TAG, "fetchSvgBytes: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val startTime = System.currentTimeMillis()
        val response = client.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "响应耗时: ${elapsed}ms, 状态码: ${response.code}")

        if (!response.isSuccessful) {
            throw GithubChartException(
                "HTTP ${response.code}: 获取贡献图失败",
                response.code
            )
        }
        val body = response.body?.bytes()
            ?: throw GithubChartException("响应体为空")
        Log.d(TAG, "响应大小: ${body.size} bytes")
        return body
    }
}

class GithubChartException(
    message: String,
    val httpCode: Int = -1
) : Exception(message)
