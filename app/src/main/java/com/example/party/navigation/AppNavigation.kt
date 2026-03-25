package com.example.party.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.party.ui.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val vm: PartyViewModel = viewModel()

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") { FeedScreen(vm, navController) }
        composable("wallet") { WalletScreen(vm, navController) }
    }
}