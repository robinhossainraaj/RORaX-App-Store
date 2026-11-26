package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RoraxApp
import com.example.data.local.AppNotification
import com.example.data.model.AppItem
import com.example.data.repository.StoreUiState
import com.example.domain.InstallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RoraxApp).container
    private val repository = container.storeRepository
    private val settingsStore = container.settingsStore

    val uiState: StateFlow<StoreUiState> = repository.uiState
    val installStates: StateFlow<Map<String, InstallState>> = repository.installStates

    val themeMode: StateFlow<String> = settingsStore.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "dark"
    )

    val notificationsEnabled: StateFlow<Boolean> = settingsStore.notificationsEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val unreadNotificationsCount: StateFlow<Int> = settingsStore.unreadNotificationsCountFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val notificationsHistory: StateFlow<List<AppNotification>> = settingsStore.notificationHistoryFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _isSyncingNotifications = MutableStateFlow(false)
    val isSyncingNotifications = _isSyncingNotifications.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadStore(forceRefresh = false)
            container.syncScheduledNotifications()
        }
    }

    fun syncNotifications() {
        viewModelScope.launch {
            _isSyncingNotifications.value = true
            container.syncScheduledNotifications()
            _isSyncingNotifications.value = false
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.loadStore(forceRefresh = true)
            container.syncScheduledNotifications()
        }
    }

    fun recheckInstalled() {
        repository.recheckInstalledStates()
    }

    fun handleInstallClick(app: AppItem, onRequestPermission: () -> Unit) {
        if (!repository.canRequestPackageInstalls()) {
            onRequestPermission()
            return
        }
        repository.startDownloadAndInstall(app)
    }

    fun cancelDownload(app: AppItem) {
        repository.cancelDownload(app)
    }

    fun openApp(packageName: String, onCannotOpen: () -> Unit) {
        val opened = repository.launchApp(packageName)
        if (!opened) {
            onCannotOpen()
        }
    }

    fun updateAll() {
        val state = uiState.value
        if (state is StoreUiState.Success) {
            val updateCandidates = state.apps.filter { app ->
                installStates.value[app.packageName] is InstallState.UpdateAvailable
            }
            repository.updateAll(updateCandidates)
        }
    }

    fun setTheme(mode: String) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setNotificationsEnabled(enabled)
            container.schedulePeriodicUpdateCheck(enabled)
        }
    }

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            settingsStore.markAllNotificationsAsRead()
        }
    }
}
