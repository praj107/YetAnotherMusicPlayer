package com.yamp.data.repository

import com.yamp.data.local.db.dao.LibraryCollectionOrderDao
import com.yamp.domain.model.LibraryCollectionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LibraryCollectionOrderRepository @Inject constructor(
    private val orderDao: LibraryCollectionOrderDao
) {
    fun observeTabOrder(type: LibraryCollectionType): Flow<List<String>> =
        orderDao.observeTabOrder(type.name).map { order -> order.map { it.itemKey } }

    suspend fun saveTabOrder(type: LibraryCollectionType, itemKeys: List<String>) {
        orderDao.replaceTabOrder(type.name, itemKeys)
    }
}
