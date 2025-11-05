package com.example.absensiapk


import CopyrightBottomBar
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.absensiapk.ui.theme.Abu
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins
import androidx.navigation.NavController
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.api.RetrofitClient
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import coil.compose.rememberImagePainter
import com.example.absensiapk.models.AttendanceData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale


@Composable
fun AttendanceListScreen(navController: NavController, homeViewModel: HomeViewModel = viewModel()) {
    val attendanceList by homeViewModel.attendanceList.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadAttendanceList()
    }

    Scaffold(
        topBar = { AttendanceListTopBar(navController = navController) },
        bottomBar = { CopyrightBottomBar()}
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BluePAL)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(attendanceList){
                record -> AttendanceListCard(record = record)}
        }
    }
}

@Composable
fun AttendanceListTopBar(navController: NavController){
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
            painter = painterResource(id = R.drawable.back_arrow), // Ganti dengan ikon panah balik Anda
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable{ navController.popBackStack() }
        )
        Text(
            text = "List Absensi",
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
fun AttendanceListCard(record: AttendanceData) {
    val localeIndonesia = Locale("in", "ID")
    val formattedDate = record.tanggal?.let{
        try {
            val date = LocalDate.parse(it)
            date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", localeIndonesia))
        } catch (e: DateTimeParseException) {
            it
        }
    }?:"-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Bagian Tanggal
            Text(
                text = formattedDate,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                if (record.jamMasuk != null) {
                    AttendanceStatusItem(
                        title = "Check In",
                        time = record.jamMasuk,
                        status = record.statusMasuk,
                        locationLat = record.lokasiMasukLat,
                        locationLong = record.lokasiMasukLong,
                        photoUrl = record.fotoMasuk?.let { "${RetrofitClient.BASE_URL}$it"}
                    )
                }

                if(record.jamMasuk!=null && record.jamKeluar != null) {
                    VerticalDivider()
                }

                if (record.jamKeluar != null) {
                    AttendanceStatusItem(
                        title = "Check Out",
                        time = record.jamKeluar,
                        status = record.statusKeluar,
                        locationLat = record.lokasiKeluarLat,
                        locationLong = record.lokasiKeluarLong,
                        photoUrl = record.fotoKeluar?.let { "${RetrofitClient.BASE_URL}$it"}
                    )
                }
            }
        }
    }
}



@Composable
fun AttendanceStatusItem(
    title: String,
    time: String,
    status: String?,
    locationLat: Double?,
    locationLong: Double?,
    photoUrl: String?
) {
    val timeColor = when (status) {
        "On Time"-> Color.Green
        "Telat","Pulang Cepat" -> Color.Red
        else -> Color.Gray
    }

    val displayStatus = when (status) {
        "On Time" -> "On Time"
        "Telat" -> "Telat"
        "Pulang Cepat" -> "Pulang Cepat"
        else -> "-"
    }

    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Waktu & Status
        if (!time.isNullOrEmpty()) {
            Text(
                text = time,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = timeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayStatus,
                fontFamily = Poppins,
                fontSize = 12.sp,
                color = timeColor
            )
        } else {
            Text(
                text = "-",
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Red
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (!photoUrl.isNullOrEmpty()) {
            Image(
                painter = rememberImagePainter(photoUrl),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (locationLat != null && locationLong != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.location),
                    contentDescription = "Location",
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$locationLat, $locationLong",
                    fontFamily = Poppins,
                    fontSize = 8.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(180.dp)
            .background(Color.Black)
    )
}


