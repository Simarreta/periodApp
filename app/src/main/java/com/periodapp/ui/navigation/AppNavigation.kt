package com.periodapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.combine
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.periodapp.data.OnboardingDataStore
import com.periodapp.notification.NotificationScheduler
import com.periodapp.ui.main.MainScreen
import com.periodapp.ui.wizard.WizardScreen

private const val START_ROUTE = "start"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dataStore = remember(context) { OnboardingDataStore(context.applicationContext) }

    NavHost(
        navController = navController,
        startDestination = START_ROUTE
    ) {
        composable(START_ROUTE) {
            val hasPeriodData by combine(
                dataStore.isOnboardingComplete,
                dataStore.getLastPeriodStartMs()
            ) { isComplete, lastPeriodStartMs ->
                isComplete || lastPeriodStartMs != null
            }.collectAsState(initial = null)
            LaunchedEffect(hasPeriodData) {
                if (hasPeriodData == true) {
                    navController.navigate(MAIN_ROUTE) {
                        popUpTo(START_ROUTE) { inclusive = true }
                    }
                } else if (hasPeriodData == false) {
                    navController.navigate(WIZARD_ROUTE) {
                        popUpTo(START_ROUTE) { inclusive = true }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        composable(WIZARD_ROUTE) {
            WizardScreen(
                onComplete = {
                    navController.navigate(MAIN_ROUTE) {
                        popUpTo(WIZARD_ROUTE) { inclusive = true }
                    }
                },
                dataStore = dataStore
            )
        }
        composable(MAIN_ROUTE) {
            LaunchedEffect(Unit) {
                NotificationScheduler.schedule(context.applicationContext, dataStore)
            }
            MainScreen(dataStore = dataStore)
        }
    }
}
