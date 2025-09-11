import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.absensiapk.R
import com.example.absensiapk.models.AttendanceData
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins
import com.google.android.gms.location.LocationServices
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Composable
fun CheckOutScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var userLocation by remember { mutableStateOf("Mendapatkan Lokasi..") }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }

    val tempUri = remember(context) {
        val file = File.createTempFile("image", ".jpg", context.externalCacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = "${location.latitude}, ${location.longitude}"
                } else {
                    userLocation = "Lokasi tidak tersedia"
                }
            }
        } else {
            userLocation = "Izin lokasi ditolak"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedImageUri = tempUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLon = location.longitude
                        userLocation = "${location.latitude}, ${location.longitude}"
                    }
                }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            cameraLauncher.launch(tempUri)
        } else {
            Toast.makeText(context,"Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        topBar = { CheckOutTopBar(navController) },
        bottomBar = { CopyrightBottomBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            PhotoPreviewBox(imageUri= capturedImageUri)
            Spacer(modifier = Modifier.height(32.dp))
            if (capturedImageUri == null) {
                TakePhotoButton(
                    text = "Take Photo",
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            } else {
                TakePhotoButton(
                    text = "Submit",
                    onClick = {
                        val currentTime = LocalTime.now()
                        val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        val checkOutHour = currentTime.hour
                        val checkOutMinute = currentTime.minute
                        val status = if ((checkOutHour == 15 && checkOutMinute >= 30) || (checkOutHour == 16 && checkOutMinute <= 30)) {
                            "On Time"
                        } else {
                            "Telat"
                        }

                        val attendanceId = homeViewModel.todayAttendance.value.id
                        Log.d("ABSENSI_CHECKOUT", "Absensi ID dari SharedPreferences: $attendanceId")

                        if (attendanceId != null && capturedImageUri != null && userLat != null && userLon != null) {
                            homeViewModel.submitCheckOut(
                                time = formattedTime,
                                status = status,
                                lat = userLat!!,
                                lon = userLon!!,
                                photoUri = capturedImageUri!!
                            )
                            navController.navigate("home")
                        } else {
                            Toast.makeText(context, "Data tidak lengkap", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
            LocationSection(locationText = userLocation)
        }
    }
}

@Composable
fun CheckOutTopBar(navController: NavController) {
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
                .clickable { navController.popBackStack() }
        )
        Text(
            text = "Check Out",
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
        )
        // Spacer kosong untuk menyeimbangkan layout
        Spacer(modifier = Modifier.size(24.dp))
    }
}


