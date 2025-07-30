package com.qali.barista.data

import kotlinx.coroutines.flow.Flow

class FoodItemRepository(private val foodItemDao: FoodItemDao) {
    
    fun getAllItems(): Flow<List<FoodItem>> = foodItemDao.getAllItems()
    
    suspend fun getItemById(id: Long): FoodItem? = foodItemDao.getItemById(id)
    
    suspend fun getItemByBarcode(barcode: String): FoodItem? = foodItemDao.getItemByBarcode(barcode)
    
    suspend fun insertItem(item: FoodItem): Long = foodItemDao.insertItem(item)
    
    suspend fun updateItem(item: FoodItem) = foodItemDao.updateItem(item)
    
    suspend fun deleteItem(item: FoodItem) = foodItemDao.deleteItem(item)
    
    suspend fun deleteItemById(id: Long) = foodItemDao.deleteItemById(id)
}