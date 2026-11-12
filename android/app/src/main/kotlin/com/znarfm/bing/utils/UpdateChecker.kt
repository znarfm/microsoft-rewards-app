package com.znarfm.bing.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.znarfm.bing.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/znarfm/microsoft-rewards-app/releases/latest"
    private const val USER_AGENT = "com.znarfm.bing"
    private const val TIMEOUT_MS = 10_000
    private const val BUFFER_SIZE = 8192

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val releaseNotes: String,
        val htmlUrl: String,
        val downloadUrl: String,
        val assetName: String
    )

    fun isNewerVersion(currentVersion: String, latestTagOrVersion: String): Boolean {
        val currParts = parseVersionParts(currentVersion)
        val latestParts = parseVersionParts(latestTagOrVersion)

        val maxLen = maxOf(currParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val curr = currParts.getOrElse(i) { 0 }
            val latest = latestParts.getOrElse(i) { 0 }
            if (latest > curr) return true
            if (latest < curr) return false
        }
        return false
    }

    private fun parseVersionParts(versionStr: String): List<Int> {
        val clean = versionStr.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")

        return clean.split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    fun selectBestApkAsset(
        assets: List<Pair<String, String>>,
        supportedAbis: List<String>
    ): Pair<String, String>? {
        val apkAssets = assets.filter { it.first.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        for (abi in supportedAbis) {
            val match = apkAssets.firstOrNull { it.first.contains(abi, ignoreCase = true) }
            if (match != null) return match
        }

        val universal = apkAssets.firstOrNull { it.first.contains("universal", ignoreCase = true) }
        if (universal != null) return universal

        return apkAssets.firstOrNull()
    }

    fun parseLatestReleaseJson(
        jsonStr: String,
        currentVersion: String,
        supportedAbis: List<String>
    ): ReleaseInfo? {
        val tagName = extractJsonStringField(jsonStr, "tag_name") ?: return null
        val htmlUrl = extractJsonStringField(jsonStr, "html_url") ?: ""
        val body = extractJsonStringField(jsonStr, "body") ?: ""

        val assets = extractAssets(jsonStr)
        val selectedAsset = selectBestApkAsset(assets, supportedAbis) ?: return null

        val versionName = tagName.removePrefix("v").removePrefix("V")

        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            releaseNotes = body,
            htmlUrl = htmlUrl,
            downloadUrl = selectedAsset.second,
            assetName = selectedAsset.first
        )
    }

    internal fun extractJsonStringField(json: String, key: String): String? {
        val keyMarker = "\"$key\""
        val idx = json.indexOf(keyMarker)
        if (idx == -1) return null

        val colon = json.indexOf(':', idx + keyMarker.length)
        if (colon == -1) return null

        val startQuote = json.indexOf('"', colon)
        if (startQuote == -1) return null

        val sb = StringBuilder()
        var i = startQuote + 1
        var isEscaped = false
        val len = json.length

        while (i < len) {
            val c = json[i]
            if (isEscaped) {
                when (c) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 4 < len) {
                            val hex = json.substring(i + 1, i + 5)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 4
                            } else {
                                sb.append(c)
                            }
                        } else {
                            sb.append(c)
                        }
                    }
                    else -> sb.append(c)
                }
                isEscaped = false
            } else {
                if (c == '\\') {
                    isEscaped = true
                } else if (c == '"') {
                    return sb.toString()
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        return null
    }

    internal fun extractAssets(json: String): List<Pair<String, String>> {
        val assetsIdx = json.indexOf("\"assets\"")
        if (assetsIdx == -1) return emptyList()

        val arrayStart = json.indexOf('[', assetsIdx)
        if (arrayStart == -1) return emptyList()

        var depth = 0
        var arrayEnd = -1
        var inString = false
        var isEscaped = false

        for (i in arrayStart until json.length) {
            val c = json[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        arrayEnd = i
                        break
                    }
                }
            }
        }

        if (arrayEnd == -1) return emptyList()
        val assetsJson = json.substring(arrayStart + 1, arrayEnd)

        val results = mutableListOf<Pair<String, String>>()
        var objDepth = 0
        var objStart = -1
        inString = false
        isEscaped = false

        for (i in assetsJson.indices) {
            val c = assetsJson[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (objDepth == 0) objStart = i
                    objDepth++
                } else if (c == '}') {
                    objDepth--
                    if (objDepth == 0 && objStart != -1) {
                        val obj = assetsJson.substring(objStart, i + 1)
                        val name = extractJsonStringField(obj, "name")
                        val downloadUrl = extractJsonStringField(obj, "browser_download_url")
                        if (!name.isNullOrBlank() && !downloadUrl.isNullOrBlank()) {
                            results.add(Pair(name, downloadUrl))
                        }
                        objStart = -1
                    }
                }
            }
        }

        return results
    }

    suspend fun fetchLatestRelease(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    throw IllegalStateException("GitHub API HTTP $responseCode: $err")
                }

                val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                val abis = Build.SUPPORTED_ABIS.toList()
                val info = parseLatestReleaseJson(jsonResponse, currentVersion, abis)

                if (info != null && isNewerVersion(currentVersion, info.tagName)) {
                    info
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val apkFile = File(updatesDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0
            val maxRedirects = 5

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", USER_AGENT)
                    instanceFollowRedirects = false
                }

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location != null && redirects < maxRedirects) {
                        currentUrl = location
                        redirects++
                        continue
                    } else {
                        throw IllegalStateException("Too many redirects or missing location: $code")
                    }
                }

                if (code !in 200..299) {
                    connection.disconnect()
                    throw IllegalStateException("Download failed with HTTP $code")
                }
                break
            }

            try {
                val contentLength = connection.contentLength.toLong()
                val inputStream: InputStream = connection.inputStream
                val outputStream = FileOutputStream(apkFile)

                outputStream.use { out ->
                    inputStream.use { inp ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (inp.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0L) {
                                val progress = (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                onProgress(progress)
                            }
                        }
                    }
                }
                apkFile
            } finally {
                connection.disconnect()
            }
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val manageIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(manageIntent)
                return false
            }
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
        return true
    }
}
