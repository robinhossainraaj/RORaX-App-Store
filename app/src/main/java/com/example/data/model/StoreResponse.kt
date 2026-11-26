package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreResponse(
    val store: StoreInfo = StoreInfo(),
    val apps: List<AppItem> = emptyList()
)
