package com.example.absensiapk


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.absensiapk.models.TimeOffData
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.ui.theme.Abu
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Orange
import com.example.absensiapk.ui.theme.Poppins
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimeOffList(navController: NavController, homeViewModel: HomeViewModel = viewModel()){
    val timeOffList by homeViewModel.timeOffList.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadTimeOffList()
    }

    Scaffold(
        topBar = { TimeOffTopBar(navController = navController) },
        bottomBar = { CopyrightBottomBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BluePAL)
                .padding(16.dp)
        ) {
            items(timeOffList) { timeOff ->
                TimeOffCard(timeOff = timeOff)
            }
        }
    }
}

@Composable
fun TimeOffTopBar(navController: NavController){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BluePAL)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
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
            text = "Time Off List",
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
fun TimeOffCard(timeOff: TimeOffData) {
    val formattedDate = timeOff.tanggalMulai?.let{
      try {
          val date = LocalDate.parse(it)
          date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
      } catch (e: Exception) {
          it
      }
  } ?: "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formattedDate,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Jenis Time Off",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    Text(
                        text = timeOff.jenis ?: "-",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Status",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = timeOff.status ?: "-",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = when (timeOff.status) {
                            "Approved" -> Color.Green
                            "Rejected" -> Color.Red
                            else -> Orange
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun CopyrightBottomBar() {
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