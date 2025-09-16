 package com.example.absensiapk.modelview

 import android.app.Application
 import android.content.Context
 import android.net.Uri
    import android.util.Log
    import androidx.core.content.edit
    import androidx.lifecycle.AndroidViewModel
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import com.example.absensiapk.api.ApiService
    import com.example.absensiapk.models.AbsenceCountData
    import com.example.absensiapk.models.AttendanceData
    import com.example.absensiapk.models.KaryawanData
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch
    import okhttp3.MediaType.Companion.toMediaTypeOrNull
    import okhttp3.MultipartBody
    import okhttp3.RequestBody.Companion.asRequestBody
    import okhttp3.RequestBody.Companion.toRequestBody
    import java.io.File
    import java.io.FileOutputStream
    import java.time.LocalDate
    import java.lang.Exception
 import java.time.format.DateTimeFormatter

 class HomeViewModel(private val apiService: ApiService, application: Application) : AndroidViewModel(application) {
        private val context: Context = application.applicationContext

        private val _todayAttendance = MutableStateFlow(AttendanceData())
        val todayAttendance: StateFlow<AttendanceData> = _todayAttendance

        private val _attendanceList = MutableStateFlow<List<AttendanceData>>(emptyList())
        val attendanceList: StateFlow<List<AttendanceData>> = _attendanceList

        private val _employeeList = MutableStateFlow<List<KaryawanData>>(emptyList())
        val employeeList: StateFlow<List<KaryawanData>> = _employeeList

        private val _karyawanName = MutableStateFlow("")
        val karyawanName: StateFlow<String> = _karyawanName

        private val _karyawanFoto = MutableStateFlow <String?>(null)
        val karyawanFoto: StateFlow<String?> = _karyawanFoto

        private val _absenceCount = MutableStateFlow<AbsenceCountData?>(null)
        val absenceCount: StateFlow<AbsenceCountData?> = _absenceCount

        private val _karyawanId = MutableStateFlow<Int?>(null)

        fun setKaryawanId(id: Int) {
            _karyawanId.value = id
            loadTodayAttendanceFromBackend()
            loadKaryawanData()
        }

        fun loadAbsenceCount(){
            viewModelScope.launch {
                try {
                    val response = apiService.getAbsenceCount()
                    if(response.isSuccessful){
                        _absenceCount.value = response.body()
                    }
                } catch (e: Exception){
                    Log.e("HomeViewModel", "Error loading absence count: ${e.message}")
                    _absenceCount.value = null
                    }
            }
        }


        private fun loadKaryawanData() {
            viewModelScope.launch {
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val karyawanId = prefs.getInt("karyawan_id", 0)
                if (karyawanId != 0) {
                    try {
                        val response = apiService.getKaryawanDetail(karyawanId)
                        if (response.isSuccessful && response.body() != null) {
                            _karyawanName.value = response.body()!!.nama
                            _karyawanFoto.value = response.body()!!.fotoProfil
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error loading karyawan data: ${e.message}")
                    }
                }
            }
        }


        fun loadEmployeeList(){
            viewModelScope.launch {
                try {
                    val response = apiService.getEmployeeList()
                    if (response.isSuccessful && response.body() != null) {
                        _employeeList.value = response.body()!!
                    } else{
                        Log.e("HomeViewModel", "Gagal memuat daftar karyawan: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error memuat daftar karyawan: ${e.message}")
                }
            }
        }

        fun loadAttendanceList() {
            viewModelScope.launch {
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val karyawanId = prefs.getInt("karyawan_id", 0)

                if (karyawanId != 0) {
                    try {
                        val response = apiService.getAttendanceHistory(karyawanId)
                        if (response.isSuccessful && response.body() != null) {
                            _attendanceList.value = response.body()!!
                        }
                    } catch (e: Exception) {
                        // ...
                    }
                }
            }
        }

        private fun loadTodayAttendanceFromBackend() {
            viewModelScope.launch {
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val karyawanId = prefs.getInt("karyawan_id", 0)

                if (karyawanId != 0) {
                    try {
                        val response = apiService.getAttendanceHistory(karyawanId)
                        if (response.isSuccessful && response.body() != null) {
                            val historyList = response.body()

                            Log.d("LOAD_ATTENDANCE", "Data riwayat diterima: ${historyList?.size} entri")

                            val todayAttendanceRecord = historyList?.find { record ->
                                Log.d("LOAD_ATTENDANCE", "Mengecek entri: ${record.tanggal}")
                                val recordDate = record.tanggal?.let {
                                    LocalDate.parse(it)
                                }
                                recordDate?.isEqual(LocalDate.now()) ?: false
                            }

                            if (todayAttendanceRecord != null) {
                                _todayAttendance.value = todayAttendanceRecord
                                Log.d("LOAD_ATTENDANCE", "Data hari ini ditemukan. Memperbarui UI.")
                                context.getSharedPreferences("absensi_prefs", Context.MODE_PRIVATE).edit {
                                    putInt("attendance_id", todayAttendanceRecord.id ?: 0)
                                    putString("last_checkin_date", todayAttendanceRecord.tanggal)
                                }
                            } else{
                                Log.d("LOAD_ATTENDANCE", "Data hari ini tidak ditemukan.")
                            }
                        } else{
                            Log.e("LOAD_ATTENDANCE", "Gagal memuat riwayat: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error loading attendance history: ${e.message}")
                    }
                }
            }
        }

        private fun uriToFile(context: Context, uri: Uri): File {
            val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return file
        }

        fun submitCheckIn(karyawanId: Int, time: String, status: String, lat: Double, lon: Double, photoUri: Uri) {
            viewModelScope.launch {
                try {
                    val tempFile = uriToFile(context, photoUri)
                    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val fotoPart = MultipartBody.Part.createFormData("foto_masuk", tempFile.name, requestFile)

                    val response = apiService.checkIn(
                        karyawan = karyawanId.toString().toRequestBody(),
                        jamMasuk = time.toRequestBody(),
                        statusMasuk = status.toRequestBody(),
                        lokasiMasukLat = lat.toString().toRequestBody(),
                        lokasiMasukLong = lon.toString().toRequestBody(),
                        fotoMasuk = fotoPart
                    )

                    if (response.isSuccessful) {
                        val attendanceRecord = response.body()
                        val photoUrl = attendanceRecord?.fotoMasuk
                        _todayAttendance.value = _todayAttendance.value.copy(
                            id = attendanceRecord?.id,
                            karyawanId = karyawanId,
                            jamMasuk = time,
                            statusMasuk = status,
                            fotoMasuk = photoUrl,
                            lokasiMasukLat = lat,
                            lokasiMasukLong = lon,
                        )

                        val prefs = context.getSharedPreferences("absensi_prefs", Context.MODE_PRIVATE)
                        prefs.edit{
                            putString("last_checkin_date", LocalDate.now().toString())
                            putInt("attendance_id", attendanceRecord?.id ?: 0)
                            apply()
                        }
                        Log.e("API_ERROR", "Gagal mengirim data: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("API_CRASH", "Kesalahan jaringan: ${e.message}")
                }
            }
        }

        fun submitCheckOut(time: String, status: String, lat: Double, lon: Double, photoUri: Uri) {
            viewModelScope.launch {
                val prefs = context.getSharedPreferences("absensi_prefs", Context.MODE_PRIVATE)
                val attendanceId = prefs.getInt("attendance_id", 0)
                Log.d("ABSENSI_CHECKOUT_VM", "Mencoba checkout untuk Absensi ID: $attendanceId")

                if (attendanceId == 0) {
                    Log.e("HomeViewModel", "Tidak ada ID absensi untuk checkout.")
                    return@launch
                }
                try {
                    val file = uriToFile(context, photoUri)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val fotoPart = MultipartBody.Part.createFormData("foto_keluar", file.name, requestFile)
                    Log.d("CHECKOUT_DEBUG", "Mencoba mengirim foto dengan nama: ${fotoPart.body.contentType()}")

                    val response = apiService.checkOut(
                        id = attendanceId,
                        jamKeluar = time.toRequestBody(),
                        statusKeluar = status.toRequestBody(),
                        lokasiKeluarLat = lat.toString().toRequestBody(),
                        lokasiKeluarLong = lon.toString().toRequestBody(),
                        fotoKeluar = fotoPart
                    )

                    if (response.isSuccessful) {
                        val attendanceRecord = response.body()
                        val photoUrlKeluar = attendanceRecord?.fotoKeluar
                        _todayAttendance.value = _todayAttendance.value.copy(
                            jamKeluar = time,
                            statusKeluar = status,
                            fotoKeluar = photoUrlKeluar,
                            lokasiKeluarLat = lat,
                            lokasiKeluarLong = lon
                        )
                    }else {
                        Log.e("HomeViewModel", "Gagal checkout: ${response.code()} - ${response.errorBody()?.string()}")}
                } catch (e: Exception) {
                    Log.e("API_CRASH", "Kesalahan jaringan: ${e.message}")
                }
            }
        }

        fun submitTimeOffRequest(
            karyawanId: Int,
            jenisTimeOff: String,
            tanggalMulai: String,
            tanggalSelesai: String,
            alasan: String
        ) {
            viewModelScope.launch {
                try {
                    val formattedTanggalMulai = tanggalMulai.format(DateTimeFormatter.ofPattern("YYYY-MM-DD"))
                    val formattedTanggalSelesai = tanggalSelesai.format(DateTimeFormatter.ofPattern("YYYY-MM-DD"))

                    val karyawanIdPart = karyawanId.toString().toRequestBody("text/plain".toMediaTypeOrNull()!!)
                    val jenisTimeOffPart = jenisTimeOff.toRequestBody("text/plain".toMediaTypeOrNull()!!)
                    val tanggalMulaiPart = formattedTanggalMulai.toRequestBody()
                    val tanggalSelesaiPart = formattedTanggalSelesai.toRequestBody()
                    val alasanPart = alasan.toRequestBody("text/plain".toMediaTypeOrNull()!!)

                    val response = apiService.timeoff(
                        karyawan = karyawanId,
                        jenis = jenisTimeOff,
                        tanggalMulai = tanggalMulai,
                        tanggalSelesai = tanggalSelesai,
                        alasan = alasan
                    )

                    if (response.isSuccessful) {
                        Log.d("API_SUCCESS", "Pengajuan cuti berhasil!")
                    } else {
                        Log.e("API_ERROR", "Pengajuan cuti gagal: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("API_CRASH", "Kesalahan saat pengajuan cuti: ${e.message}", e)
                }
            }
        }




    companion object {
        fun Factory(apiService: ApiService, application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(apiService, application) as T
                }
            }
    }
}
