package com.menmapro.iptv.data.network

import com.menmapro.iptv.data.model.Category
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.data.model.XtreamAccount
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class XtreamClient(private val httpClient: HttpClient) {

    private fun logInfo(message: String) {
        println("[XtreamClient] INFO: $message")
    }
    
    private fun logError(message: String, error: Throwable? = null) {
        println("[XtreamClient] ERROR: $message")
        error?.let { 
            println("[XtreamClient] ERROR: ${it.message}")
            println("[XtreamClient] ERROR: Stack trace: ${it.stackTraceToString()}")
        }
    }

    suspend fun authenticate(account: XtreamAccount): Boolean {
        logInfo("Authenticating Xtream account: server='${account.serverUrl}', username='${account.username}'")
        return try {
            val response: XtreamAuthResponse = httpClient.get("${account.serverUrl}/player_api.php") {
                parameter("username", account.username)
                parameter("password", account.password)
            }.body()
            val isAuthenticated = response.user_info.auth == 1
            if (isAuthenticated) {
                logInfo("Successfully authenticated Xtream account: username='${account.username}'")
            } else {
                logError("Authentication failed for Xtream account: username='${account.username}', auth=${response.user_info.auth}")
            }
            isAuthenticated
        } catch (e: HttpRequestTimeoutException) {
            logError("Request timeout during Xtream authentication: server='${account.serverUrl}'", e)
            false
        } catch (e: ConnectTimeoutException) {
            logError("Connection timeout during Xtream authentication: server='${account.serverUrl}'", e)
            false
        } catch (e: SocketTimeoutException) {
            logError("Socket timeout during Xtream authentication: server='${account.serverUrl}'", e)
            false
        } catch (e: Exception) {
            logError("Failed to authenticate Xtream account: server='${account.serverUrl}', username='${account.username}'", e)
            false
        }
    }

    suspend fun getLiveCategories(account: XtreamAccount): List<Category> {
        logInfo("Fetching live categories from Xtream server: '${account.serverUrl}'")
        return try {
            val response: List<XtreamCategory> = httpClient.get("${account.serverUrl}/player_api.php") {
                parameter("username", account.username)
                parameter("password", account.password)
                parameter("action", "get_live_categories")
            }.body()
            val categories = response.map { category ->
                Category(
                    id = category.category_id,
                    name = category.category_name,
                    parentId = if (category.parent_id != 0) category.parent_id.toString() else null
                )
            }
            logInfo("Successfully fetched ${categories.size} live categories from Xtream server")
            categories
        } catch (e: HttpRequestTimeoutException) {
            logError("Request timeout while fetching live categories from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        } catch (e: ConnectTimeoutException) {
            logError("Connection timeout while fetching live categories from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        } catch (e: SocketTimeoutException) {
            logError("Socket timeout while fetching live categories from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        } catch (e: Exception) {
            logError("Failed to fetch live categories from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        }
    }

    suspend fun getLiveStreams(account: XtreamAccount): List<Channel> {
        logInfo("Fetching live streams from Xtream server: '${account.serverUrl}'")
        return try {
            val response: List<XtreamStream> = httpClient.get("${account.serverUrl}/player_api.php") {
                parameter("username", account.username)
                parameter("password", account.password)
                parameter("action", "get_live_streams")
            }.body()

            val channels = response.map { stream ->
                Channel(
                    id = stream.stream_id.toString(),
                    name = stream.name,
                    url = "${account.serverUrl}/live/${account.username}/${account.password}/${stream.stream_id}.ts",
                    logoUrl = stream.stream_icon,
                    group = stream.category_id,
                    categoryId = stream.category_id
                )
            }
            logInfo("Successfully fetched ${channels.size} live streams from Xtream server")
            channels
        } catch (e: HttpRequestTimeoutException) {
            logError("Request timeout while fetching live streams from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        } catch (e: ConnectTimeoutException) {
            logError("Connection timeout while fetching live streams from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        } catch (e: SocketTimeoutException) {
            logError("Socket timeout while fetching live streams from Xtream server: '${account.serverUrl}'", e)
            emptyList()
        } catch (e: Exception) {
            logError("Failed to fetch live streams from Xtream server: '${account.serverUrl}'", e)
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
