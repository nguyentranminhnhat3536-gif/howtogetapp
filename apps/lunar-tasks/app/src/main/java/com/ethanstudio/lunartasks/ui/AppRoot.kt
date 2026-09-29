package com.ethanstudio.lunartasks.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ethanstudio.lunartasks.ui.edit.EditTaskScreen
import com.ethanstudio.lunartasks.ui.edit.EditTaskViewModel
import com.ethanstudio.lunartasks.ui.list.TaskListScreen

private const val ROUTE_LIST = "list"
private const val ROUTE_EDIT = "edit/{${EditTaskViewModel.ARG_TASK_ID}}"

private fun editRoute(taskId: Long) = "edit/$taskId"

/** Hai màn hình: danh sách việc và thêm/sửa việc (id = 0 là việc mới). */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            TaskListScreen(
                onAddTask = { navController.navigate(editRoute(0)) },
                onOpenTask = { id -> navController.navigate(editRoute(id)) },
            )
        }
        composable(
            ROUTE_EDIT,
            arguments = listOf(navArgument(EditTaskViewModel.ARG_TASK_ID) { type = NavType.LongType }),
        ) {
            EditTaskScreen(onClose = { navController.popBackStack(ROUTE_LIST, inclusive = false) })
        }
    }
}
