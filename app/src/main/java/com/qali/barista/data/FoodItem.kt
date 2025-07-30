package com.qali.barista.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val description: String = "",
    val imageUrl: String? = null,
    val model3dUrl: String? = null,
    val barcode: String? = null,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)