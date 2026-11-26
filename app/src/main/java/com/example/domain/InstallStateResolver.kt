package com.example.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.AppItem

class InstallStateResolver(private val context: Context) {

    fun getInstalledPackageInfo(packageName: String): Pair<Long, String>? {
        return try {
            val packageManager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            val name = info.versionName ?: ""
            code to name
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun resolve(app: AppItem, currentRunningState: InstallState? = null): InstallState {
        // If an active download or installation is running in memory, preserve it
        if (currentRunningState is InstallState.Downloading || currentRunningState is InstallState.Installing) {
            return currentRunningState
        }

        val info = getInstalledPackageInfo(app.packageName)
        val installedCode = info?.first

        return when {
            installedCode == null -> InstallState.NotInstalled
            installedCode < app.versionCode -> InstallState.UpdateAvailable(installedCode)
            else -> InstallState.UpToDate
        }
    }

    fun isInstalled(packageName: String): Boolean {
        return getInstalledPackageInfo(packageName) != null
    }

    fun canOpen(packageName: String): Boolean {
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }
}
