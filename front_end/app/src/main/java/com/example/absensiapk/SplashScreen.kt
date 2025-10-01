package com.example.absensiapk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.absensiapk.ui.theme.AbsensiAPKTheme
import com.example.absensiapk.ui.theme.Poppins
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo PT PAL Indonesia
            Image(
                painter = painterResource(id = R.drawable.logopal),
                contentDescription = "PAL Indonesia Logo",
                modifier = Modifier.width(500.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Teks Copyright di bagian bawah
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "Copyright © PT PAL Indonesia 2025",
                fontSize = 12.sp,
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
        }
    }

    // This block waits for 3 seconds and then executes the onTimeout lambda.
    LaunchedEffect(key1 = true) {
        delay(3000L) // Delay for 3 seconds
        onTimeout()
    }
}
