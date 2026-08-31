package com.example.data.local

import com.example.data.model.RestorationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RestorationRepository(private val dao: RestorationDao) {
    val allRestorations: Flow<List<RestorationRecord>> = dao.getAllRestorations()

    suspend fun insert(record: RestorationRecord): Long = withContext(Dispatchers.IO) {
        dao.insertRestoration(record)
    }

    suspend fun update(record: RestorationRecord) = withContext(Dispatchers.IO) {
        dao.updateRestoration(record)
    }

    suspend fun delete(record: RestorationRecord) = withContext(Dispatchers.IO) {
        dao.deleteRestoration(record)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
