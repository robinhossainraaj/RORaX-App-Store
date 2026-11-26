package com.example.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface InstallEvent {
    data class Success(val packageName: String) : InstallEvent
    data class Failed(val packageName: String, val message: String) : InstallEvent
    data class Aborted(val packageName: String) : InstallEvent
}

class InstallResultReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_COMMIT = "com.rorax.appstore.ACTION_INSTALL_COMMIT"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        private val _events = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 64)
        val events = _events.asSharedFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, context.getString(R.string.installed_successfully), Toast.LENGTH_SHORT).show()
                _events.tryEmit(InstallEvent.Success(packageName))
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // User cancelled the prompt, revert state silently
                _events.tryEmit(InstallEvent.Aborted(packageName))
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                val msg = context.getString(R.string.error_signature_mismatch)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                _events.tryEmit(InstallEvent.Failed(packageName, msg))
            }
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                val msg = context.getString(R.string.error_incompatible)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                _events.tryEmit(InstallEvent.Failed(packageName, msg))
            }
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val msg = context.getString(R.string.error_insufficient_storage)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                _events.tryEmit(InstallEvent.Failed(packageName, msg))
            }
            else -> {
                val extraMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val msg = if (!extraMessage.isNullOrBlank()) {
                    context.getString(R.string.error_generic_install, extraMessage)
                } else {
                    context.getString(R.string.error_generic_install, "Error code $status")
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                _events.tryEmit(InstallEvent.Failed(packageName, msg))
            }
        }
    }
}
