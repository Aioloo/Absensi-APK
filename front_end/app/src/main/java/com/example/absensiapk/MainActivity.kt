package com.example.absensiapk

import AbsentScreen
import CheckInScreen
import CheckOutScreen
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.absensiapk.api.RetrofitClient
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.modelview.LoginViewModel
import com.example.absensiapk.ui.theme.AbsensiAPKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbsensiAPKTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val retrofitClient = remember {RetrofitClient(context)}

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            apiService = retrofitClient.protectedApiService,
            application = application
        )
    )

    val loginViewModelFactory = LoginViewModel.Factory(
        apiService = retrofitClient.loginApiService,
        context = LocalContext.current
    )
    val loginViewModel: LoginViewModel = viewModel(factory = loginViewModelFactory)


    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("login") {
            LoginPageContent(
                onLoginSuccess = { karyawanId ->
                    homeViewModel.setKaryawanId(karyawanId) // Set ID di ViewModel
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                loginViewModel = loginViewModel
            )
        }

        composable("home") {
            HomeScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable ("attendance_list"){
            AttendanceListScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable("employee_list"){
            EmployeeListScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable("notification"){
            NotificationScreen(navController = navController)
        }

        composable("check_in"){
            CheckInScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable("check_out"){
            CheckOutScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable("absent"){
            AbsentScreen(navController = navController, homeViewModel = homeViewModel)
        }
    }
}


