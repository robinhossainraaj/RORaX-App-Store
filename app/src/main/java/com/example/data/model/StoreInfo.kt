package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreInfo(
    val developerName: String = "",
    val developerBio: String = "",
    val telegram: String = "",
    val email: String = "",
    val facebook: String = ""
)
