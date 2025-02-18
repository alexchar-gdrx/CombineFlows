package com.example.combineflows

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isFetchingCoupon = MutableStateFlow(false)

    fun login() {
        viewModelScope.launch {
            _isLoggedIn.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoggedIn.value = false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val homeData: StateFlow<String> = isLoggedIn
        .flatMapLatest { isLoggedIn ->
            flow {
                delay(1000) // Simulating API call
                emit(
                    if (!isLoggedIn) 1 else 2
                )
            }.onStart {
                _isFetchingCoupon.update { true }
            }
        }.onEach {
            _isFetchingCoupon.update { false }
        }
        .map { it.toString() }
        .stateIn(viewModelScope, SharingStarted.Lazily, "Initial Loading")

    val state: StateFlow<Pair<Color, String>> =
        combine(isLoggedIn, homeData, _isFetchingCoupon)
        { isLoggedIn, homeData, isFetchingCoupon ->
            // isFetchingCoupon not used in our project
            mapToUiState(false, homeData, isLoggedIn)
        }.stateIn(viewModelScope, SharingStarted.Lazily, Color.White to "Initial Loading")

    private suspend fun mapToUiState(
        isFetching: Boolean,
        homeData: String,
        isLoggedIn: Boolean
    ): Pair<Color, String> {
        delay(500) // This causes intermediate wrong state

        return when {
            isFetching || homeData.contains("Initial Loading") -> Color.White to "Initial Loading"
            isLoggedIn && homeData.contains("2") -> Color.Green to "Free User\n with\n Free Coupon"
            !isLoggedIn && homeData.contains("2") -> Color.Red to "Guest User \n with\n Free Coupon" // This case might not be intended, consider removing or changing it.
            isLoggedIn && homeData.contains("1") -> Color.Red to "Free User\n with\n Guest Coupon"
            !isLoggedIn && homeData.contains("1") -> Color.Green to "Guest User\n with\n Guest Coupon"
            else -> Color.Red to "ERROR"
        }
    }
}




