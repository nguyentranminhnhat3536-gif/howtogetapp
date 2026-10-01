package com.example.factorytemplate

import com.example.factorytemplate.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

/** Mẫu unit test: logic trong ViewModel được kiểm tra mà không cần điện thoại. */
class HomeViewModelTest {
    @Test
    fun tapIncrementsCounter() {
        val viewModel = HomeViewModel()

        viewModel.onTap()
        viewModel.onTap()

        assertEquals(2, viewModel.uiState.value.taps)
    }
}
