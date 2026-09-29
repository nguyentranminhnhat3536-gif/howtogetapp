package com.ethanstudio.lunartasks.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ethanstudio.lunartasks.LunarTasksApp
import com.ethanstudio.lunartasks.ui.edit.EditTaskViewModel
import com.ethanstudio.lunartasks.ui.list.TaskListViewModel
import com.ethanstudio.lunartasks.ui.medicine.AddMedicineViewModel
import com.ethanstudio.lunartasks.ui.settings.DisplaySettingsViewModel

/** Tạo ViewModel kèm repository lấy từ Application. */
object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            TaskListViewModel(app().repository, app().settings)
        }
        initializer {
            EditTaskViewModel(app().repository, createSavedStateHandle())
        }
        initializer {
            DisplaySettingsViewModel(app().settings)
        }
        initializer {
            AddMedicineViewModel(app().repository)
        }
    }

    private fun androidx.lifecycle.viewmodel.CreationExtras.app(): LunarTasksApp =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LunarTasksApp
}
