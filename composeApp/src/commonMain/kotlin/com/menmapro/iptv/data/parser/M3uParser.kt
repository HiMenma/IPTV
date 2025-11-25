package com.menmapro.iptv.data.parser

import com.menmapro.iptv.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M3uParser {

    suspend fun parse(content: String): List<Channel> = withContext(Dispatchers.Default) {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentId: String? = null
        
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Example: #EXTINF:-1 tvg-id="CNN" tvg-logo="http://logo.png" group-title="News",CNN International
                val info = trimmed.substringAfter("#EXTINF:")
                
                // Extract Name (last part after comma)
                currentName = info.substringAfterLast(",").trim()
                
                // Extract Logo
                val logoRegex = "tvg-logo=\"([^\"]*)\"".toRegex()
                currentLogo = logoRegex.find(info)?.groupValues?.get(1)
                
                // Extract Group
                val groupRegex = "group-title=\"([^\"]*)\"".toRegex()
                currentGroup = groupRegex.find(info)?.groupValues?.get(1)
                
                // Extract ID
                val idRegex = "tvg-id=\"([^\"]*)\"".toRegex()
                currentId = idRegex.find(info)?.groupValues?.get(1)
                
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This is the URL
                if (currentName != null) {
                    channels.add(
                        Channel(
                            id = currentId ?: trimmed, // Fallback ID
                            name = currentName!!,
                            url = trimmed,
                            logoUrl = currentLogo,
                            group = currentGroup
                        )
                    )
                    // Reset for next channel
                    currentName = null
                    currentLogo = null
                    currentGroup = null
                    currentId = null
                }
            }
        }
        
        channels
    }
}
