package com.example.install

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface DownloadEvent {
    data class Progress(val percent: Int) : DownloadEvent
    data class Done(val file: File) : DownloadEvent
    data class Failed(val message: String) : DownloadEvent
}

class ApkDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    fun getApkFile(packageName: String, versionCode: Long): File {
        val apksDir = File(context.getExternalFilesDir(null), "apks")
        if (!apksDir.exists()) {
            apksDir.mkdirs()
        }
        return File(apksDir, "$packageName-$versionCode.apk")
    }

    fun isApkAlreadyDownloaded(packageName: String, versionCode: Long): Boolean {
        val file = getApkFile(packageName, versionCode)
        return file.exists() && file.length() > 0
    }

    fun downloadApk(url: String, packageName: String, versionCode: Long): Flow<DownloadEvent> = flow {
        val targetFile = getApkFile(packageName, versionCode)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

        // If a verified file already exists, immediately return Done
        if (targetFile.exists() && targetFile.length() > 1024) {
            emit(DownloadEvent.Done(targetFile))
            return@flow
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "RORAX-AppStore/1.0")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadEvent.Failed("Download failed with HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadEvent.Failed("Empty response body from server"))
                return@flow
            }

            val contentLength = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    var lastPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (contentLength > 0) {
                            val percent = ((downloadedBytes * 100) / contentLength).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                emit(DownloadEvent.Progress(percent))
                            }
                        } else {
                            emit(DownloadEvent.Progress(-1)) // Indeterminate
                        }
                    }
                    output.flush()
                }
            }

            // Rename temp to target
            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                if (tempFile.renameTo(targetFile)) {
                    emit(DownloadEvent.Done(targetFile))
                } else {
                    emit(DownloadEvent.Failed("Failed to save downloaded APK file."))
                }
            } else {
                emit(DownloadEvent.Failed("Downloaded APK file missing."))
            }

        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            emit(DownloadEvent.Failed(e.message ?: "Download connection interrupted"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteApk(packageName: String, versionCode: Long) {
        try {
            val file = getApkFile(packageName, versionCode)
            if (file.exists()) file.delete()
            val temp = File(file.parentFile, "${file.name}.tmp")
            if (temp.exists()) temp.delete()
        } catch (_: Exception) {}
    }
}
