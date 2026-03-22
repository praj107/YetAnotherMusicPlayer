package com.yamp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yamp.data.local.db.entity.LibraryCollectionOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryCollectionOrderDao {
    @Query("SELECT * FROM library_collection_order WHERE tabType = :tabType ORDER BY sortOrder ASC")
    fun observeTabOrder(tabType: String): Flow<List<LibraryCollectionOrderEntity>>

    @Query("DELETE FROM library_collection_order WHERE tabType = :tabType")
    suspend fun deleteTabOrder(tabType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<LibraryCollectionOrderEntity>)

    @Transaction
    suspend fun replaceTabOrder(tabType: String, itemKeys: List<String>) {
        deleteTabOrder(tabType)
        insertOrders(
            itemKeys.mapIndexed { index, key ->
                LibraryCollectionOrderEntity(
                    tabType = tabType,
                    itemKey = key,
                    sortOrder = index
                )
            }
        )
    }
}
