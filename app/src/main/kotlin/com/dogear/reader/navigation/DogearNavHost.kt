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

/** Top-level navigation graph. New feature destinations (reader, settings, upload) slot in here. */
object Routes {
    const val LIBRARY = "library"
    const val BOOK_DETAIL = "book/{bookId}"
    fun bookDetail(bookId: Long): String = "book/$bookId"
}

@Composable
fun DogearNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) },
            )
        }
        composable(
            route = Routes.BOOK_DETAIL,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) {
            BookDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
