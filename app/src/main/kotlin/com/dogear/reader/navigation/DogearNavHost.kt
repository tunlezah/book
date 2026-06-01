package com.dogear.reader.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dogear.reader.feature.library.ui.BookDetailScreen
import com.dogear.reader.feature.library.ui.LibraryScreen
import com.dogear.reader.feature.reader.ui.ReaderScreen
import com.dogear.reader.feature.settings.SettingsScreen
import com.dogear.reader.feature.upload.UploadScreen

/** Top-level navigation graph. */
object Routes {
    const val LIBRARY = "library"
    const val BOOK_DETAIL = "book/{bookId}"
    const val READER = "reader/{bookId}"
    const val UPLOAD = "upload"
    const val SETTINGS = "settings"
    fun bookDetail(bookId: Long): String = "book/$bookId"
    fun reader(bookId: Long): String = "reader/$bookId"
}

@Composable
fun DogearNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) },
                onOpenUpload = { navController.navigate(Routes.UPLOAD) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.BOOK_DETAIL,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) { entry ->
            val bookId = entry.arguments?.getLong("bookId") ?: 0L
            BookDetailScreen(
                onBack = { navController.popBackStack() },
                onRead = { navController.navigate(Routes.reader(bookId)) },
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) {
            ReaderScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.UPLOAD) {
            UploadScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
