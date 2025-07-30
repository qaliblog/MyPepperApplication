package com.qali.barista.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<FoodItem>>
    
    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getItemById(id: Long): FoodItem?
    
    @Query("SELECT * FROM food_items WHERE barcode = :barcode")
    suspend fun getItemByBarcode(barcode: String): FoodItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoodItem): Long
    
    @Update
    suspend fun updateItem(item: FoodItem)
    
    @Delete
    suspend fun deleteItem(item: FoodItem)
    
    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}