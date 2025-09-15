import android.content.Context
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.absensiapk.R
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.ui.theme.AbsensiAPKTheme
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOffScreen(navController: NavController, homeViewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedJenisTimeOff by remember { mutableStateOf("TimeOff") }
    var alasan by remember { mutableStateOf("") }
    var tanggalMulai by remember { mutableStateOf<LocalDate?>(null) }
    var tanggalSelesai by remember { mutableStateOf<LocalDate?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val jenisTimeOffList = listOf("Cuti", "Sakit", "Izin")
    var isExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TimeOffTopBar(navController = navController)}) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = !isExpanded }
            ) {
                TextField(
                    value = selectedJenisTimeOff,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Time Off") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                    jenisTimeOffList.forEach { jenis ->
                        DropdownMenuItem(
                            text = { Text(jenis) },
                            onClick = {
                                selectedJenisTimeOff = jenis
                                isExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Field Tanggal Mulai dan Selesai (tampilkan untuk semua jenis)
            DatePickerField(label = "Tanggal Mulai", selectedDate = tanggalMulai) { tanggalMulai = it }
            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(label = "Tanggal Selesai", selectedDate = tanggalSelesai) { tanggalSelesai = it }

            // Field alasan (tampilkan untuk semua jenis)
            OutlinedTextField(
                value = alasan,
                onValueChange = { alasan = it },
                label = { Text("Alasan") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    val karyawanId = prefs.getInt("karyawan_id", 0)

                    if(tanggalMulai == null || tanggalSelesai == null || alasan.isEmpty() || karyawanId == 0){
                        Toast.makeText(context, "Harap lengkapi semua data.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if(tanggalMulai!! > tanggalSelesai!!){
                        Toast.makeText(context, "Tanggal mulai tidak boleh lebih besar dari tanggal selesai.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    homeViewModel.submitTimeOffRequest()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text("Ajukan Permintaan")
            }
        }
    }
}

@Composable
fun TimeOffTopBar(navController: NavController){
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
            text = "Time Off",
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
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val year = selectedDate?.year ?: calendar.get(Calendar.YEAR)
    val month = selectedDate?.monthValue?.minus(1) ?: calendar.get(Calendar.MONTH)
    val day = selectedDate?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val newDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(newDate)
        }, year, month, day
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                datePickerDialog.show()
            }
    ) {
        OutlinedTextField(
            value = selectedDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = "Pilih Tanggal",
                    modifier = Modifier.clickable {
                        datePickerDialog.show()
                    }
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimeOffScreenPreview(){
    // Memberikan data dummy untuk preview
    AbsensiAPKTheme {
        TimeOffScreen(
            navController = rememberNavController(),
            homeViewModel = viewModel()
        )
    }
}