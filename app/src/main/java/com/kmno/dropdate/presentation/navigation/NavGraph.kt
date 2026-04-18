package com.kmno.dropdate.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kmno.dropdate.presentation.schedule.ScheduleScreen

@Composable
fun DropDateNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Schedule) {
        composable<Screen.Schedule> {
            ScheduleScreen()
        }
    }
}
