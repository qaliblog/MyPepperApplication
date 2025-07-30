package com.qali.barista.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qali.barista.data.FoodItem
import com.qali.barista.data.FoodItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BaristaViewModel(private val repository: FoodItemRepository) : ViewModel() {
    
    private val _items = MutableStateFlow<List<FoodItem>>(emptyList())
    val items: StateFlow<List<FoodItem>> = _items.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadItems()
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            repository.getAllItems().collect { items ->
                _items.value = items
            }
        }
    }
    
    fun addItem(name: String, price: Double, description: String = "", barcode: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val item = FoodItem(
                    name = name,
                    price = price,
                    description = description,
                    barcode = barcode
                )
                repository.insertItem(item)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteItem(item: FoodItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }
    
    fun searchItemByBarcode(barcode: String, onFound: (FoodItem) -> Unit, onNotFound: () -> Unit) {
        viewModelScope.launch {
            val item = repository.getItemByBarcode(barcode)
            if (item != null) {
                onFound(item)
            } else {
                onNotFound()
            }
        }
    }
}

class BaristaViewModelFactory(private val repository: FoodItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BaristaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BaristaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}