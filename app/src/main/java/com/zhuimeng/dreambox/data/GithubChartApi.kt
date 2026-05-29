package com.zhuimeng.dreambox.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * GitHub 贡献图 API
 *
 * 从 ghchart.rshah.org 获取 SVG 格式的贡献热力图。
 * 无需 API Key，无需 OAuth。
 *
 * URL 格式: https://ghchart.rshah.org/{color}/{username}
 * - color: 十六进制颜色值（不含 #），如 198754 表示绿色
 * - username: GitHub 用户名
 */
object GithubChartApi {

    private const val BASE_URL = "https://ghchart.rshah.org"
    private const val TIMEOUT_SECONDS = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 获取贡献热力图 SVG 的原始字节
     *
     * @param color 十六进制颜色（不含 #）
     * @param username GitHub 用户名
     * @return SVG 文件的字节数组
     */
    suspend fun fetchSvgBytes(color: String, username: String): ByteArray {
        val url = "$BASE_URL/$color/$username"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw GithubChartException(
                "HTTP ${response.code}: 获取贡献图失败",
                response.code
            )
        }
        return response.body?.bytes()
            ?: throw GithubChartException("响应体为空")
    }
}

class GithubChartException(
    message: String,
    val httpCode: Int = -1
) : Exception(message)
