package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.remote.configureAppCheck
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.screens.DesignWizardScreen
import com.example.ui.screens.GardenStudioScreen
import com.example.ui.screens.GardensHomeScreen
import com.example.ui.theme.VerdantTheme
import com.example.ui.viewmodel.GardenStudioViewModel
import com.example.ui.viewmodel.GardensViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private object Routes {
    const val HOME = "gardens_home"
    const val WIZARD = "design_wizard"
    const val STUDIO = "garden_studio/{gardenId}"

    fun studio(gardenId: String): String = "garden_studio/$gardenId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before UI, ViewModels, or Firebase ops
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            VerdantTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            VerdantStudioNavHost(currentUserId = user.uid)
        }
    }
}

@Composable
private fun VerdantStudioNavHost(currentUserId: String) {
    val navController = rememberNavController()

    val gardensViewModel: GardensViewModel = viewModel(
        key = "gardens_$currentUserId",
        factory = GardensViewModel.provideFactory(currentUserId),
    )
    val studioViewModel: GardenStudioViewModel = viewModel(
        key = "studio_$currentUserId",
        factory = GardenStudioViewModel.provideFactory(currentUserId),
    )

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            GardensHomeScreen(
                viewModel = gardensViewModel,
                onCreateNewGarden = { navController.navigate(Routes.WIZARD) },
                onOpenGarden = { gardenId -> navController.navigate(Routes.studio(gardenId)) },
            )
        }

        composable(Routes.WIZARD) {
            DesignWizardScreen(
                viewModel = studioViewModel,
                onNavigateBack = { navController.popBackStack() },
                onGardenCreated = { gardenId ->
                    navController.navigate(Routes.studio(gardenId)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable(
            route = Routes.STUDIO,
            arguments = listOf(navArgument("gardenId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val gardenId = backStackEntry.arguments?.getString("gardenId").orEmpty()
            GardenStudioScreen(
                gardenId = gardenId,
                viewModel = studioViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
