package com.menmapro.iptv.data.database.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.menmapro.iptv.data.database.IptvDatabase
import com.menmapro.iptv.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EpgDao(private val database: IptvDatabase) {
    
    fun getEpgByChannelId(channelId: String, currentTime: Long): Flow<List<EpgProgram>> {
        return database.iptvDatabaseQueries
            .selectEpgByChannelId(channelId, currentTime)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { programs ->
                programs.map { dbProgram ->
                    EpgProgram(
                        id = dbProgram.id,
                        channelId = dbProgram.channelId,
                        title = dbProgram.title,
                        description = dbProgram.description,
                        startTime = dbProgram.startTime,
                        endTime = dbProgram.endTime
                    )
                }
            }
    }
    
    suspend fun getCurrentProgram(channelId: String, currentTime: Long): EpgProgram? = 
        withContext(Dispatchers.Default) {
            database.iptvDatabaseQueries
                .selectCurrentProgram(channelId, currentTime, currentTime)
                .executeAsOneOrNull()
                ?.let { dbProgram ->
                    EpgProgram(
                        id = dbProgram.id,
                        channelId = dbProgram.channelId,
                        title = dbProgram.title,
                        description = dbProgram.description,
                        startTime = dbProgram.startTime,
                        endTime = dbProgram.endTime
                    )
                }
        }
    
    suspend fun insertEpgProgram(program: EpgProgram) = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.insertEpgProgram(
            id = program.id,
            channelId = program.channelId,
            title = program.title,
            description = program.description,
            startTime = program.startTime,
            endTime = program.endTime
        )
    }
    
    suspend fun insertEpgPrograms(programs: List<EpgProgram>) = withContext(Dispatchers.Default) {
        database.transaction {
            programs.forEach { program ->
                database.iptvDatabaseQueries.insertEpgProgram(
                    id = program.id,
                    channelId = program.channelId,
                    title = program.title,
                    description = program.description,
                    startTime = program.startTime,
                    endTime = program.endTime
                )
            }
        }
    }
    
    suspend fun deleteOldEpgPrograms(beforeTime: Long) = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.deleteOldEpgPrograms(beforeTime)
    }
    
    suspend fun deleteEpgByChannelId(channelId: String) = withContext(Dispatchers.Default) {
        database.iptvDatabaseQueries.deleteEpgByChannelId(channelId)
    }
}
