package com.example.party.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import com.example.party.ui.*
import com.google.firebase.auth.FirebaseAuth

@Serializable object Router
@Serializable object Login
@Serializable object Register
@Serializable object Feed
@Serializable object Wallet
@Serializable object Map
@Serializable object Profile
@Serializable object Scanner
@Serializable object Social
@Serializable data class Details(val discoId: String)
@Serializable data class Payment(val discoId: String)

data class NavItem(val route: Any, val icon: ImageVector, val label: String)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val vm: PartyViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()
    val userRole by vm.userRole.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isLogin = currentDestination?.hierarchy?.any { it.hasRoute(Login::class) } == true
    val isRegister = currentDestination?.hierarchy?.any { it.hasRoute(Register::class) } == true
    val isRouter = currentDestination?.hierarchy?.any { it.hasRoute(Router::class) } == true
    val isPayment = currentDestination?.hierarchy?.any { it.hasRoute(Payment::class) } == true

    val showBottomBar = !isLogin && !isRegister && !isRouter && !isPayment && currentDestination != null

    val startDest: Any = if (auth.currentUser != null) Router else Login

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = when (userRole) {
                        "Staff" -> listOf(
                            NavItem(Scanner, Icons.Default.QrCodeScanner, "Escáner"),
                            NavItem(Profile, Icons.Default.Person, "Perfil")
                        )
                        "Discoteca" -> listOf(
                            NavItem(Profile, Icons.Default.Home, "Admin")
                        )
                        else -> listOf(
                            NavItem(Feed, Icons.Default.Home, "Feed"),
                            NavItem(Social, Icons.Default.People, "Social"),
                            NavItem(Map, Icons.Default.LocationOn, "Mapa"),
                            NavItem(Wallet, Icons.Default.ShoppingCart, "Wallet"),
                            NavItem(Profile, Icons.Default.Person, "Perfil")
                        )
                    }

                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = startDest, modifier = Modifier.padding(innerPadding)) {

            composable<Router> {
                val role by vm.userRole.collectAsState()
                val isLoaded by vm.profileLoaded.collectAsState()

                if (isLoaded) {
                    LaunchedEffect(role) {
                        val dest = when (role) {
                            "Staff" -> Scanner
                            "Discoteca" -> Profile
                            else -> Feed
                        }
                        navController.navigate(dest) { popUpTo<Router> { inclusive = true } }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0C29)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE91E63), modifier = Modifier.size(60.dp))
                    }
                }
            }

            composable<Login> { LoginScreen(
                onLoginSuccess = { navController.navigate(Router) { popUpTo<Login> { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Register) }
            )}
            composable<Register> { RegisterScreen(
                onRegisterSuccess = { navController.navigate(Router) { popUpTo<Register> { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() }
            )}

            composable<Feed> { FeedScreen(vm, navController) }
            composable<Wallet> { WalletScreen(vm, navController) }
            composable<Map> { MapScreen(vm, navController) }
            composable<Profile> { ProfileScreen(vm, navController) }
            composable<Scanner> { StaffScannerScreen(vm) }
            composable<Social> { SocialScreen(vm, navController) }

            composable<Details> { backStackEntry ->
                val details: Details = backStackEntry.toRoute()
                EventDetailsScreen(details.discoId, vm, navController)
            }

            composable<Payment> { backStackEntry ->
                val payData: Payment = backStackEntry.toRoute()
                PaymentScreen(payData.discoId, vm, navController)
            }
        }
    }
}