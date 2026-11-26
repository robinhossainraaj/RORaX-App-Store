package com.example.domain

sealed interface InstallState {
    data object NotInstalled : InstallState
    data class UpdateAvailable(val installedCode: Long) : InstallState
    data object UpToDate : InstallState
    data class Downloading(val progress: Int) : InstallState // 0..100 (-1 for indeterminate)
    data object Installing : InstallState
}
