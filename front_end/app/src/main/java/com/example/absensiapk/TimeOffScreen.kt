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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.absensiapk.R
import com.example.absensiapk.modelview.HomeViewModel
import com.example.absensiapk.ui.theme.BluePAL
import com.example.absensiapk.ui.theme.Poppins
import java.time.format.DateTimeParseException
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOffScreen(navController: NavController, homeViewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    var alasan by remember { mutableStateOf("") }

    var tanggalMulai by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var tanggalSelesai by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    var isSubmitting by remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }
    val jatahCuti by homeViewModel.jatahCuti.collectAsState()
    val sisaCuti by homeViewModel.sisaCuti.collectAsState()
    var selectedJenisTimeOff by remember { mutableStateOf("Cuti") }

    LaunchedEffect(Unit){
        homeViewModel.loadKaryawanData()
    }

    Scaffold(topBar = { TimeOffTopBar(navController = navController)}) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ){
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),

            ){
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Time Off Form",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                        color = BluePAL,
                        textAlign = TextAlign.Center
                    )

                    jatahCuti?.let {
                        Text(
                            text = "Sisa Jatah Cuti Tahun Ini : ${sisaCuti ?: 0} kali.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

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
                            val dynamicJenisTimeOffList = listOf("Cuti", "Sakit", "Izin")

                            dynamicJenisTimeOffList.forEach { jenis ->
                                DropdownMenuItem(
                                    text = { Text(jenis)},
                                    onClick = {
                                        selectedJenisTimeOff = jenis
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    DatePickerField(
                        label = "Tanggal Mulai",
                        selectedDate = tanggalMulai,
                        onDateSelected = { tanggalMulai = it },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DatePickerField(
                        label = "Tanggal Selesai",
                        selectedDate = tanggalSelesai,
                        onDateSelected = { tanggalSelesai = it }
                    )

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

                            if(sisaCuti == 0){
                                Toast.makeText(context,"Sisa cuti Anda telah habis.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if(tanggalMulai!! > tanggalSelesai!!){
                                Toast.makeText(context, "Tanggal mulai tidak boleh lebih besar dari tanggal selesai.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmitting = true
                            homeViewModel.submitTimeOffRequest(
                                jenisTimeOff = selectedJenisTimeOff,
                                tanggalMulai = tanggalMulai!!.toString(),
                                tanggalSelesai = tanggalSelesai!!.toString(),
                                alasan = alasan
                            )
                            navController.navigate("home")
                        },
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePAL),
                        enabled = !isSubmitting
                    ) {
                        Text(
                            text = "Ajukan Permintaan",
                            color = Color.White,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                            )
                    }
                }
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

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = selectedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                selection = TextRange(0)
            )
        )
    }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val newDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(newDate)
            textFieldValue = TextFieldValue(
                text = newDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                selection = TextRange(0)
            )
        },
        selectedDate?.year ?: calendar.get(Calendar.YEAR),
        selectedDate?.monthValue?.minus(1) ?: calendar.get(Calendar.MONTH),
        selectedDate?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable (onClickLabel = "Pilih Tanggal $label"){
                datePickerDialog.show()
            }
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                newValue ->textFieldValue = newValue
                try{
                    val parsedDate = LocalDate.parse(newValue.text, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    onDateSelected(parsedDate)
                } catch (e: DateTimeParseException){
                    // Tanggal tidak valid, Anda dapat menambahkan logika penanganan kesalahan di sini
                }

            },
            label = { Text(label) },
            placeholder = {Text("dd/MM/yyyy")},
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
