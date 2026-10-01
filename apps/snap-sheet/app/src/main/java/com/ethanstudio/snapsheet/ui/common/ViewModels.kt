package com.ethanstudio.snapsheet.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ethanstudio.snapsheet.SnapSheetApp
import com.ethanstudio.snapsheet.ui.auth.AuthViewModel
import com.ethanstudio.snapsheet.ui.doc.DocViewModel
import com.ethanstudio.snapsheet.ui.doc.PagesViewModel
import com.ethanstudio.snapsheet.ui.main.MainViewModel
import com.ethanstudio.snapsheet.ui.ocr.OcrViewModel

/** Tạo ViewModel kèm các đối tượng dùng chung lấy từ Application. */
object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { MainViewModel(app().docs, app().proStore, app().billing) }
        initializer { AuthViewModel(app().auth, app().session) }
        initializer { DocViewModel(app().docs, app().proStore, createSavedStateHandle()) }
        initializer { OcrViewModel(app(), app().docs, app().proStore, createSavedStateHandle()) }
        initializer { PagesViewModel(app().docs, createSavedStateHandle()) }
    }

    private fun CreationExtras.app(): SnapSheetApp =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SnapSheetApp
}
