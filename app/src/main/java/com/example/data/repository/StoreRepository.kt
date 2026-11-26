package com.example.data.repository

import com.example.data.local.JsonCache
import com.example.data.model.AppItem
import com.example.data.model.StoreInfo
import com.example.data.remote.StoreApi
import com.example.domain.InstallState
import com.example.domain.InstallStateResolver
import com.example.install.ApkDownloader
import com.example.install.ApkInstaller
import com.example.install.DownloadEvent
import com.example.install.InstallEvent
import com.example.install.InstallResultReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface StoreUiState {
    data object Loading : StoreUiState
    data class Success(val store: StoreInfo, val apps: List<AppItem>, val isOffline: Boolean) : StoreUiState
    data class Error(val message: String) : StoreUiState
}

class StoreRepository(
    private val storeApi: StoreApi,
    private val jsonCache: JsonCache,
    private val resolver: InstallStateResolver,
    private val apkDownloader: ApkDownloader,
    private val apkInstaller: ApkInstaller,
    private val appScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _installStates = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    val installStates: StateFlow<Map<String, InstallState>> = _installStates.asStateFlow()

    private val activeDownloadJobs = mutableMapOf<String, Job>()

    init {
        // Listen to package install completion events
        appScope.launch {
            InstallResultReceiver.events.collect { event ->
                when (event) {
                    is InstallEvent.Success -> {
                        // Re-resolve state for the installed app
                        val currentApp = getAppByPackage(event.packageName)
                        if (currentApp != null) {
                            // Delete temporary APK file upon successful install
                            apkDownloader.deleteApk(currentApp.packageName, currentApp.versionCode)
                            _installStates.update { current ->
                                current + (currentApp.packageName to InstallState.UpToDate)
                            }
                        }
                        recheckInstalledStates()
                    }
                    is InstallEvent.Failed, is InstallEvent.Aborted -> {
                        recheckInstalledStates()
                    }
                }
            }
        }
    }

    fun getAppByPackage(packageName: String): AppItem? {
        val state = _uiState.value
        return if (state is StoreUiState.Success) {
            state.apps.firstOrNull { it.packageName == packageName }
        } else {
            null
        }
    }

    suspend fun loadStore(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value is StoreUiState.Success) {
            return
        }

        if (_uiState.value !is StoreUiState.Success) {
            _uiState.value = StoreUiState.Loading
        }

        try {
            val (raw, parsed) = storeApi.fetchStoreJson()
            jsonCache.saveCache(raw)
            _uiState.value = StoreUiState.Success(
                store = parsed.store,
                apps = parsed.apps,
                isOffline = false
            )
            recheckInstalledStates(parsed.apps)
        } catch (e: Exception) {
            // Attempt to load from offline cache
            val cached = jsonCache.readCache()
            if (cached != null) {
                _uiState.value = StoreUiState.Success(
                    store = cached.store,
                    apps = cached.apps,
                    isOffline = true
                )
                recheckInstalledStates(cached.apps)
            } else {
                _uiState.value = StoreUiState.Error(
                    message = e.localizedMessage ?: "Failed to connect to store."
                )
            }
        }
    }

    fun recheckInstalledStates(appsList: List<AppItem>? = null) {
        val apps = appsList ?: (_uiState.value as? StoreUiState.Success)?.apps ?: emptyList()
        val currentStates = _installStates.value
        val newMap = apps.associate { app ->
            val currentState = currentStates[app.packageName]
            app.packageName to resolver.resolve(app, currentState)
        }
        _installStates.value = newMap
    }

    fun startDownloadAndInstall(app: AppItem) {
        // Block duplicate downloads
        val currentState = _installStates.value[app.packageName]
        if (currentState is InstallState.Downloading || currentState is InstallState.Installing) {
            return
        }

        val job = appScope.launch(Dispatchers.IO) {
            _installStates.update { it + (app.packageName to InstallState.Downloading(0)) }

            apkDownloader.downloadApk(app.apkUrl, app.packageName, app.versionCode).collect { event ->
                when (event) {
                    is DownloadEvent.Progress -> {
                        _installStates.update { it + (app.packageName to InstallState.Downloading(event.percent)) }
                    }
                    is DownloadEvent.Done -> {
                        _installStates.update { it + (app.packageName to InstallState.Installing) }
                        apkInstaller.installApk(event.file, app.packageName)
                    }
                    is DownloadEvent.Failed -> {
                        // Re-resolve back to previous state
                        val fallback = resolver.resolve(app)
                        _installStates.update { it + (app.packageName to fallback) }
                    }
                }
            }
        }
        activeDownloadJobs[app.packageName] = job
    }

    fun cancelDownload(app: AppItem) {
        activeDownloadJobs[app.packageName]?.cancel()
        activeDownloadJobs.remove(app.packageName)
        apkDownloader.deleteApk(app.packageName, app.versionCode)
        val fallback = resolver.resolve(app)
        _installStates.update { it + (app.packageName to fallback) }
    }

    fun launchApp(packageName: String): Boolean {
        return apkInstaller.launchApp(packageName)
    }

    fun canRequestPackageInstalls(): Boolean {
        return apkInstaller.canRequestPackageInstalls()
    }

    fun getManageUnknownAppSourcesIntent() = apkInstaller.getManageUnknownAppSourcesIntent()

    fun updateAll(appsToUpdate: List<AppItem>) {
        appScope.launch(Dispatchers.IO) {
            for (app in appsToUpdate) {
                val state = _installStates.value[app.packageName]
                if (state is InstallState.UpdateAvailable) {
                    startDownloadAndInstall(app)
                    // Wait for it to finish or timeout before proceeding to next in queue
                    var waited = 0
                    while (waited < 120) {
                        kotlinx.coroutines.delay(1000)
                        val curr = _installStates.value[app.packageName]
                        if (curr !is InstallState.Downloading && curr !is InstallState.Installing) {
                            break
                        }
                        waited++
                    }
                }
            }
        }
    }
}
