package com.menmapro.iptv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val categoryId: String? = null,
    val headers: Map<String, String> = emptyMap()
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val url: String? = null,
    val type: PlaylistType,
    val channels: List<Channel> = emptyList(),
    val categories: List<Category> = emptyList(),
    val xtreamAccount: XtreamAccount? = null
)

enum class PlaylistType {
    M3U_URL, M3U_FILE, XTREAM
}

@Serializable
data class XtreamAccount(
    val serverUrl: String,
    val username: String,
    val password: String
)
