package com.menmapro.iptv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,  // Unix timestamp in milliseconds
    val endTime: Long     // Unix timestamp in milliseconds
)

data class EpgChannel(
    val channelId: String,
    val tvgId: String,
    val tvgName: String? = null
)
