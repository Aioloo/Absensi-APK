import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.absensiapk.R
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import java.io.File
import coil.compose.rememberImagePainter
import com.example.absensiapk.models.AttendanceData
import com.example.absensiapk.modelview.HomeViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect

@Composable
fun CheckInScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
){
    val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var userLocation by remember { mutableStateOf("Mendapatkan Lokasi..") }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }
    var alasan by remember { mutableStateOf("") } // <-- State untuk alasan
    var isLate by remember { mutableStateOf(false)}
    
    // State untuk location warning
    var showLocationWarningDialog by remember { mutableStateOf(false) }
    var locationWarningMessage by remember { mutableStateOf("") }
    var locationDistance by remember { mutableStateOf(0.0) }

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
                    cameraLauncher.launch(tempUri)
                }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            Toast.makeText(context, "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        topBar = { CheckInTopBar(navController) },
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
            PhotoPreviewBox(imageUri = capturedImageUri)
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
                        val checkInLimit = LocalTime.of(7, 30)
                        val status = if (currentTime.isAfter(checkInLimit)) "Telat" else "On Time"

                        if (status == "Telat" && alasan.isEmpty()){
                            isLate = true
                            Toast.makeText(context, "Isi Alasan Keterlambatan Check In", Toast.LENGTH_LONG).show()
                            return@TakePhotoButton
                        }

                        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        val karyawanId = prefs.getInt("karyawan_id", 0)

                        if (karyawanId != 0 && capturedImageUri != null && userLat != null && userLon != null) {
                            homeViewModel.submitCheckIn(
                                karyawanId = karyawanId,
                                time = formattedTime,
                                status = status,
                                lat = userLat!!,
                                lon = userLon!!,
                                photoUri = capturedImageUri!!,
                                alasan = alasan,
                                onLocationWarning = { message, distance ->
                                    // Show dialog if location is outside PT PAL area
                                    locationWarningMessage = message
                                    locationDistance = distance
                                    showLocationWarningDialog = true
                                },
                                onSuccess = {
                                    // Navigate to home only if no warning
                                    navController.navigate("home")
                                }
                            )
                        } else {
                            Toast.makeText(context, "Data tidak lengkap", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
            LocationSection(locationText = userLocation)
            if (isLate){
                OutlinedTextField(
                    value = alasan,
                    onValueChange = {alasan = it},
                    label = { Text("Alasan Keterlambatan (Wajib)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Location Warning Dialog
        if (showLocationWarningDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showLocationWarningDialog = false
                    navController.navigate("home")
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Peringatan Lokasi",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = locationWarningMessage,
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Jarak dari PT PAL: ${String.format("%.2f", locationDistance)} km",
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF5722)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Absensi Anda telah tersimpan dengan status 'Di Luar Area PT PAL' untuk keperluan monitoring.",
                            fontFamily = Poppins,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLocationWarningDialog = false
                            navController.navigate("home")
                        }
                    ) {
                        Text(
                            text = "Mengerti",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            color = BluePAL
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun CheckInTopBar(navController: NavController) {
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
            text = "Check In",
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)// Membuat teks mengisi ruang di tengah
        )
        // Spacer kosong untuk menyeimbangkan layout
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
fun PhotoPreviewBox(imageUri: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberImagePainter(imageUri),
                contentDescription = "Captured Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Tampilan placeholder
            Text("No Photo Taken", color = Color.Gray)
        }
    }
}

@Composable
fun TakePhotoButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePAL),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

@Composable
fun LocationSection(locationText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.location),
            contentDescription = "Location",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = locationText,
                fontFamily = Poppins,
                fontSize = 14.sp,
                color = Color.Black
            )
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
