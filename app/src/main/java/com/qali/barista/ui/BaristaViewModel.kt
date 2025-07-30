package com.qali.barista.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Simple in-memory data class
class FoodItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val price: Double,
    val description: String = "",
    val barcode: String? = null
)

class BaristaViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<FoodItem>>(emptyList())
    val items: StateFlow<List<FoodItem>> = _items.asStateFlow()

    fun addItem(name: String, price: Double, description: String = "", barcode: String? = null) {
        val item = FoodItem(
            name = name,
            price = price,
            description = description,
            barcode = barcode
        )
        _items.value = _items.value + item
    }

    fun deleteItem(item: FoodItem) {
        _items.value = _items.value.filterNot { it.id == item.id }
    }

    fun searchItemByBarcode(barcode: String, onFound: (FoodItem) -> Unit, onNotFound: () -> Unit) {
        val item = _items.value.find { it.barcode == barcode }
        if (item != null) {
            onFound(item)
        } else {
            onNotFound()
        }
    }
}