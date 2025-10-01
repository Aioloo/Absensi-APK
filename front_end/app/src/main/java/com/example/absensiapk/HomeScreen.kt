package com.example.absensiapk

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.absensiapk.ui.theme.Abu
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Orange
import com.example.absensiapk.ui.theme.Poppins
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalTime
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.absensiapk.api.RetrofitClient
import com.example.absensiapk.models.AbsenceCountData
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.modelview.LoginViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


private val tickerFlow: Flow<LocalTime> = flow {
    while (true) {
        emit(LocalTime.now())
        delay(1000)
    }
}.distinctUntilChanged { old, new ->
    old.hour == new.hour && old.minute == new.minute
}

enum class AttendanceState {
    CHECK_IN, CHECK_OUT, IDLE
}

private fun getAttendanceState(currentTime: LocalTime): AttendanceState {
    val checkInStart = LocalTime.of(6, 0)
    val checkInEnd = LocalTime.of(12, 0)
    val checkOutStart = LocalTime.of(15, 30)
    val checkOutEnd = LocalTime.of(19, 0)

    return when {
        currentTime.isAfter(checkOutStart) && currentTime.isBefore(checkOutEnd) -> AttendanceState.CHECK_OUT
        currentTime.isAfter(checkInStart) && currentTime.isBefore(checkInEnd) -> AttendanceState.CHECK_IN
        else -> AttendanceState.IDLE
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val currentTime by tickerFlow.collectAsState(initial = LocalTime.now())
    val karyawanName by homeViewModel.karyawanName.collectAsState()
    val karyawanFoto by homeViewModel.karyawanFoto.collectAsState()
    val absenceCounts by homeViewModel.absenceCount.collectAsState()


    LaunchedEffect(Unit) {
        homeViewModel.loadAbsenceCount()
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.75f),
                drawerContainerColor = BluePAL
            ) {
                Column (
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ){
                    Column{
                        Text("Menu",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            color = Color.White)
                        Divider()
                    }
                    TextButton(
                        onClick = {
                           loginViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                            homeViewModel.resetData()
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            Text("Logout",
                                fontFamily = Poppins,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.logout),
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    karyawanFoto = karyawanFoto,
                    onMenuClick = { scope.launch {drawerState.open()}}
                )
                     },
            bottomBar = { LogoBawah() }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(color = Abu)
                    .verticalScroll(scrollState)
            ) {
                HeaderSection(navController, currentTime = currentTime, karyawanName = karyawanName)
                Spacer(modifier = Modifier.height(16.dp))
                AbsenceCountSection(absenceCounts)
                Spacer(modifier = Modifier.height(16.dp))
                TodayAttendanceSection(homeViewModel)
                TombolBawah(navController = navController)
            }
        }
    }
}

@Composable
fun TopAppBar(karyawanFoto: String?, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(BluePAL)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick =onMenuClick ) {
            Icon(
                painter = painterResource(id = R.drawable.menu),
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Image(
            painter = if (karyawanFoto.isNullOrEmpty()){
                painterResource(id = R.drawable.propil)
            } else {
                rememberImagePainter(karyawanFoto)
            },
            contentDescription = "User Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun HeaderSection(navController: NavController, currentTime: LocalTime, karyawanName: String) {
    val attendanceState = getAttendanceState(currentTime)
    val isCheckOutTime = (attendanceState == AttendanceState.CHECK_OUT)

    val titleText = when (attendanceState) {
        AttendanceState.CHECK_IN -> "Check In Time !!"
        AttendanceState.CHECK_OUT -> "Check Out Time !!"
        else -> "Outside Absent Hours"
    }

    val displayTimeStart = if (isCheckOutTime) "15:30" else "06:00"
    val displayTimeEnd = if (isCheckOutTime) "19:00" else "12:00"

    val isCheckInEnabled = attendanceState == AttendanceState.CHECK_IN
    val isCheckOutEnabled = attendanceState == AttendanceState.CHECK_OUT


    val checkInbuttonColor = when (attendanceState) {
        AttendanceState.CHECK_IN -> Color.Green
        AttendanceState.CHECK_OUT -> Color.Gray
        else -> Color.Gray
    }

    val checkOutbuttonColor = when (attendanceState) {
        AttendanceState.CHECK_IN -> Color.Gray
        AttendanceState.CHECK_OUT -> Color.Red
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BluePAL)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Good Morning",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = karyawanName,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)

        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (attendanceState == AttendanceState.IDLE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "X",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeDisplay(time = displayTimeStart)
                        Text(text = "-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        TimeDisplay(time = displayTimeEnd)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { navController.navigate("check_in") },
                        enabled = isCheckInEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = checkInbuttonColor),
                        modifier = Modifier.size(width = 130.dp, height = 50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.fingerprint),
                                contentDescription = "Check In", tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Check In", fontFamily = Poppins, color = Color.Black)
                        }
                    }
                    Button(
                        onClick = { navController.navigate("check_out")},
                        enabled = isCheckOutEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = checkOutbuttonColor),
                        modifier = Modifier.size(width = 145.dp, height = 50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.fingerprint),
                                contentDescription = "Check Out", tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Check Out",
                                fontFamily = Poppins,
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {navController.navigate("time_off")},
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    modifier = Modifier.size(width = 130.dp, height = 50.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.run),
                            contentDescription = "Time Off",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Time Off",
                            fontFamily = Poppins,
                            color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeDisplay(time: String) {
        Text(
        text = time,
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    )
}

@Composable
fun AbsenceCountSection(counts: AbsenceCountData?) {
    counts?.let {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                Modifier
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Total Kehadiran",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = "${it.totalKehadiran}",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }


                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Text(
                        text = "Total Timeoff",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "${it.totalTimeoff}",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

        }
    }
}

@Composable
fun TodayAttendanceSection(homeViewModel: HomeViewModel = viewModel()) {
    val todayAttendance by homeViewModel.todayAttendance.collectAsState()
    val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Attendance",
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = todayDate,
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusItem(
                    statusText = "Check In",
                    photoUri = todayAttendance.fotoMasuk?.let {Uri.parse("${RetrofitClient.BASE_URL}$it")},
                    attendanceTime = todayAttendance.jamMasuk ?: "-",
                    coordinate = if (todayAttendance.lokasiMasukLat != null) "${todayAttendance.lokasiMasukLat}, ${todayAttendance.lokasiMasukLong}" else "-",
                    isAttended = todayAttendance.jamMasuk != null,
                    status = todayAttendance.statusMasuk
                )
                VerticalDivider()

                StatusItem(
                    statusText = "Check Out",
                    photoUri = todayAttendance.fotoKeluar?.let {Uri.parse("${RetrofitClient.BASE_URL}$it")},
                    attendanceTime = todayAttendance.jamKeluar ?: "-",
                    coordinate = if (todayAttendance.lokasiKeluarLat != null) "${todayAttendance.lokasiKeluarLat}, ${todayAttendance.lokasiKeluarLong}" else "-",
                    isAttended = todayAttendance.jamKeluar != null,
                    status = todayAttendance.statusKeluar
                )
            }
        }
    }
}



@Composable
fun StatusItem(
    statusText: String,
    photoUri: Uri?,
    attendanceTime: String,
    coordinate: String,
    isAttended: Boolean,
    status: String?
) {

    val timeColor = when (status) {
        "On Time" -> Color.Green
        "Telat" -> Color.Red
        else -> Color.Gray
    }

    val displayStatus = when (status) {
        "On Time" -> "On Time"
        "Telat" -> "Telat"
        else -> "-"
    }

    Column(
        modifier = Modifier.width(90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isAttended && photoUri != null) {
            val imagePainter = rememberImagePainter(data = photoUri)

            Text(
                text = attendanceTime,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = timeColor,
            )
            Text(
                text = displayStatus,
                fontFamily = Poppins,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = timeColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = imagePainter,
                contentDescription = statusText,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .background(Color.LightGray)
            )
            Text(
                text = coordinate,
                fontFamily = Poppins,
                fontSize = 10.sp,
                color = Color.Black,
                textAlign = TextAlign.Left
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-",
                    fontFamily = Poppins,
                    fontSize = 24.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TombolBawah(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .background(Abu)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = R.drawable.clock,
                label = "Attendance List",
                iconColor = BluePAL,
                onClick = { navController.navigate("attendance_list") }
            )
            BottomNavItem(
                icon = R.drawable.person,
                label = "Employee's List",
                iconColor = Color.Green,
                onClick = { navController.navigate("employee_list") }
            )
            BottomNavItem(
                icon = R.drawable.timeofflist,
                label = "Time Off List",
                iconColor = Orange,
                onClick = { navController.navigate("time_off_list") }
            )
        }

    }
}

@Composable
fun BottomNavItem(icon: Int, label: String, iconColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = label,
                fontFamily = Poppins,
                fontSize = 10.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun LogoBawah() {
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





