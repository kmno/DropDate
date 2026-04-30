package com.kmno.dropdate.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kmno.dropdate.presentation.schedule.ScheduleScreen
import com.kmno.dropdate.presentation.tracked.TrackedReleasesScreen

@Composable
fun DropDateNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Schedule,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(200),
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(200),
            ) + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(200),
            ) + fadeIn(tween(200))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(200),
            )
        },
    ) {
        composable<Screen.Schedule> {
            ScheduleScreen(
                onNavigateToTrackings = {
                    navController.navigate(Screen.Trackings)
                },
            )
        }

        composable<Screen.Trackings> {
            TrackedReleasesScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
