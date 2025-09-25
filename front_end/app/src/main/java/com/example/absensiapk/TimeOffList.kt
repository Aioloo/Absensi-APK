package com.example.absensiapk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.absensiapk.ui.theme.AbsensiAPKTheme
import com.example.absensiapk.ui.theme.Abu
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins

data class NotificationItem(
    val time : String,
    val message : String
)

@Composable
fun NotificationScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { NotificationTopBar(navController) },
        bottomBar = { NotificationBottomBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(color = Abu)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Data statis untuk contoh
            val notifications = listOf(
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)"),
                NotificationItem("06.30", "Jangan Lupa Absen Masuk Ya!! (06.30 - 12.00)")
            )

            notifications.forEach { notification ->
                NotificationCard(notification = notification)
            }
        }
    }
}

@Composable
fun NotificationTopBar(navController: NavController){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(BluePAL)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(id = R.drawable.back_arrow),
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable{navController.popBackStack()}
        )
        Text(
            text = "Notification",
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun NotificationCard(notification: NotificationItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = notification.time,
                fontFamily = Poppins,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = notification.message,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun NotificationBottomBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BluePAL)
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

