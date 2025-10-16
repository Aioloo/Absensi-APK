package com.example.absensiapk

import CopyrightBottomBar
import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.absensiapk.api.RetrofitClient
import com.example.absensiapk.models.KaryawanData
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


@Composable
fun EmployeeListScreen(navController: NavController){
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            RetrofitClient(context).protectedApiService,
            context.applicationContext as Application
        )
    )
    val employeeList by homeViewModel.employeeList.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadEmployeeList()
    }

    Scaffold(
        topBar = { EmployeeListTopBar(navController = navController) },
        bottomBar = { CopyrightBottomBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BluePAL)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(employeeList) { employee ->
                EmployeeCard(employee = employee)
            }
        }
    }
}

@Composable
fun EmployeeListTopBar(navController: NavController){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BluePAL)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Icon(
            painter = painterResource(id = R.drawable.back_arrow), // Ganti dengan ikon panah balik Anda
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable{ navController.popBackStack() }
        )
        Text(
            text = "Employee's List",
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
        )
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun EmployeeCard(employee: KaryawanData){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp) ,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.nama,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow("Divisi", employee.divisi?:"-")
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Email", employee.email?:"-")
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Perusahaan", employee.perusahaan?.nama ?: "-")
            }

            if(!employee.fotoProfil.isNullOrEmpty()){
                Image(
                    painter = rememberImagePainter(employee.fotoProfil),
                    contentDescription = "Foto Profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp, 100.dp)
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label : ",
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Color.Black
        )
        Text(
            text = value,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}
