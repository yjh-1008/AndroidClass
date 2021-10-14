package com.example.repository_pattern

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MyViewModel(context: Context) : ViewModel() {

    private val repository = MyRepository(context)

    val repos = repository.repos

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                repository.refreshData()
            } catch (e: Exception) {
                Log.e("Network","Failed to connect to the server!")
            }
        }
    }


    class Factory(val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MyViewModel(context) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }

}
