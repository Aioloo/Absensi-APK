package com.example.absensiapk

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.absensiapk.modelview.LoginState
import com.example.absensiapk.modelview.LoginViewModel
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins


@Composable
fun LoginPageContent(onLoginSuccess: (Int) -> Unit, loginViewModel: LoginViewModel) {
    var nip by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }

    val loginState by loginViewModel.loginState.collectAsState()

    LaunchedEffect(Unit) {
        loginViewModel.resetLoginState()
    }


    LaunchedEffect(key1 = loginState) {
        if (loginState is LoginState.Success) {
            val karyawanId = (loginState as LoginState.Success).karyawanId
            Log.d("LoginPage", "Login berhasil, karyawan ID: $karyawanId")
            onLoginSuccess(karyawanId)
        }
    }

    Scaffold(
        topBar = { LoginHeader() }, // <-- Topbar baru
        bottomBar = { LoginBottomBar() } // <-- Bottombar baru
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Login",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    color = BluePAL
                )
                Text(
                    text = "Let's Sign In First :)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Poppins,
                    color = BluePAL
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            LoginCardContent(
                nip = nip,
                onNipChange = { nip = it },
                password = password,
                onPasswordChange = { password = it },
                passwordVisibility = passwordVisibility,
                onPasswordVisibilityToggle = { passwordVisibility = !passwordVisibility },
                onLoginClick = { loginViewModel.login(nip, password) },
                loginState = loginState
            )
        }
    }
}

@Composable
fun LoginHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.header_login),
            contentDescription = "Header Background",
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
fun LoginBottomBar() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = BluePAL)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Copyright © PT PAL Indonesia 2025",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = Poppins
            )
        }
    }
}

@Composable
fun LoginCardContent(
    nip: String,
    onNipChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisibility: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    loginState: LoginState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "NIP", fontSize = 16.sp, color = BluePAL, fontFamily = Poppins, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            OutlinedTextField(
                value = nip,
                onValueChange = onNipChange,
                label = { Text("Masukkan NIP") },
                modifier = Modifier.fillMaxWidth(),
                enabled = loginState !is LoginState.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BluePAL)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Masukkan Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                enabled = loginState !is LoginState.Loading
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lihat Password",
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { onPasswordVisibilityToggle() },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePAL),
                enabled = loginState !is LoginState.Loading
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Login",
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            if (loginState is LoginState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    }
}