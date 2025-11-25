package com.menmapro.iptv.data.network

import com.menmapro.iptv.data.model.Category
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.XtreamAccount
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class XtreamClient(private val httpClient: HttpClient) {

    suspend fun authenticate(account: XtreamAccount): Boolean {
        return try {
            val response: XtreamAuthResponse = httpClient.get("${account.serverUrl}/player_api.php") {
                parameter("username", account.username)
                parameter("password", account.password)
            }.body()
            response.user_info.auth == 1
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLiveCategories(account: XtreamAccount): List<Category> {
        return try {
            val response: List<XtreamCategory> = httpClient.get("${account.serverUrl}/player_api.php") {
                parameter("username", account.username)
                parameter("password", account.password)
                parameter("action", "get_live_categories")
            }.body()
            response.map { category ->
                Category(
                    id = category.category_id,
                    name = category.category_name,
                    parentId = if (category.parent_id != 0) category.parent_id.toString() else null
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveStreams(account: XtreamAccount): List<Channel> {
        return try {
            val response: List<XtreamStream> = httpClient.get("${account.serverUrl}/player_api.php") {
                parameter("username", account.username)
                parameter("password", account.password)
                parameter("action", "get_live_streams")
            }.body()

            response.map { stream ->
                Channel(
                    id = stream.stream_id.toString(),
                    name = stream.name,
                    url = "${account.serverUrl}/live/${account.username}/${account.password}/${stream.stream_id}.ts",
                    logoUrl = stream.stream_icon,
                    group = stream.category_id
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Serializable
data class XtreamAuthResponse(
    val user_info: XtreamUserInfo
)

@Serializable
data class XtreamUserInfo(
    val auth: Int
)

@Serializable
data class XtreamCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)

@Serializable
data class XtreamStream(
    val stream_id: Int,
    val name: String,
    val stream_icon: String?,
    val category_id: String?
)
