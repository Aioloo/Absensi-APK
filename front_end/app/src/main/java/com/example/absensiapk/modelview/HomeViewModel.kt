 package com.example.absensiapk.modelview

 import android.app.Application
 import android.content.Context
 import android.graphics.Bitmap
 import android.graphics.BitmapFactory
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
 import com.example.absensiapk.models.TimeOffData
 import kotlinx.coroutines.flow.MutableStateFlow
 import kotlinx.coroutines.flow.StateFlow
 import kotlinx.coroutines.launch
 import okhttp3.MediaType.Companion.toMediaTypeOrNull
 import okhttp3.MultipartBody
 import okhttp3.RequestBody.Companion.asRequestBody
 import okhttp3.RequestBody.Companion.toRequestBody
 import java.io.ByteArrayOutputStream
 import java.io.File
 import java.io.FileOutputStream
 import java.io.InputStream
 import java.time.LocalDate
 import java.lang.Exception

 private const val MAX_UPLOAD_SIZE = 10 * 1024
 private const val MAX_WIDTH = 640

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

     private val _timeOffList = MutableStateFlow<List<TimeOffData>>(emptyList())
     val timeOffList: StateFlow<List<TimeOffData>> = _timeOffList

     private val _jatahCuti = MutableStateFlow<Int?>(null)
     val jatahCuti: StateFlow<Int?> = _jatahCuti

     private val _sisaCuti = MutableStateFlow<Int?>(null)
     val sisaCuti: StateFlow<Int?> = _sisaCuti

     // Location Warning State
     private val _locationWarning = MutableStateFlow<Map<String, Any>?>(null)
     val locationWarning: StateFlow<Map<String, Any>?> = _locationWarning

     private val _karyawanId = MutableStateFlow<Int?>(null)

     fun setKaryawanId(id: Int) {
         _karyawanId.value = id

         val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
         prefs.edit {
             putInt("karyawan_id", id)
             apply()
         }

         loadTodayAttendanceFromBackend()
         loadKaryawanData()
     }

     fun resetData() {
         _todayAttendance.value = AttendanceData()
         _attendanceList.value = emptyList()
         _employeeList.value = emptyList()
         _karyawanName.value = ""
         _karyawanFoto.value = null
         _absenceCount.value = null
         _timeOffList.value = emptyList()
         _jatahCuti.value = null
         _sisaCuti.value = null
         _karyawanId.value = null

         // Hapus juga data di SharedPreferences
         context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit {
             clear()
             apply()
         }
         context.getSharedPreferences("absensi_prefs", Context.MODE_PRIVATE).edit {
             clear()
             apply()
         }
     }


     fun loadTimeOffList() {
         viewModelScope.launch {
             val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
             val karyawanId = prefs.getInt("karyawan_id", 0)

             if (karyawanId != 0) {
                 try {
                     val response = apiService.getTimeOffHistory(karyawanId)
                     if (response.isSuccessful && response.body() != null) {
                         _timeOffList.value = response.body()!!
                     }
                 } catch (e: Exception) {
                     //blablabla
                 }
             }
         }
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


        fun loadKaryawanData() {
            viewModelScope.launch {
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val karyawanId = prefs.getInt("karyawan_id", 0)
                if (karyawanId != 0) {
                    try {
                        val response = apiService.getKaryawanDetail(karyawanId)
                        if (response.isSuccessful && response.body() != null) {
                            _karyawanName.value = response.body()!!.nama
                            _karyawanFoto.value = response.body()!!.fotoProfil
                            _jatahCuti.value = response.body()!!.jatahCutiPerTahun
                            _sisaCuti.value = response.body()!!.sisaCutiPerBulan
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

         // 1. Baca Dimensi dan Downscale (Pengurangan dimensi)
         val options = BitmapFactory.Options().apply {
             inJustDecodeBounds = true
             context.contentResolver.openInputStream(uri)?.use {
                 BitmapFactory.decodeStream(it, null, this)
             }
         }

         var scaleFactor = 1
         if (options.outWidth > MAX_WIDTH) {
             scaleFactor = options.outWidth / MAX_WIDTH
         }

         val finalOptions = BitmapFactory.Options().apply {
             inSampleSize = scaleFactor
         }

         val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
         val originalBitmap = BitmapFactory.decodeStream(inputStream, null, finalOptions) ?: return file

         // 2. Lakukan Kompresi Iteratif (Menurunkan Kualitas Agresif)
         var quality = 80 // Mulai dari kualitas 80%
         var currentFile = file

         while (quality > 0) {
             val outputStream = ByteArrayOutputStream()

             // Kompresi dengan kualitas saat ini
             originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

             // Tulis byte terkompresi ke file
             try {
                 FileOutputStream(currentFile).use {
                     it.write(outputStream.toByteArray())
                 }
             } catch (e: Exception) {
                 Log.e("CompressionError", "Gagal menulis file terkompresi", e)
                 break
             }

             val fileSize = currentFile.length()

             if (fileSize <= MAX_UPLOAD_SIZE) {
                 Log.d("COMPRESSION_DEBUG", "Kompresi Selesai: Ukuran mencapai target 10 KB.")
                 break // Selesai jika sudah di bawah 10 KB
             }

             // Kurangi kualitas agresif (lompat 10 poin)
             quality -= 10
         }

         // Bersihkan objek Bitmap
         inputStream?.close()
         originalBitmap.recycle()

         return currentFile
     }

        fun submitCheckIn(
            karyawanId: Int, 
            time: String, 
            status: String, 
            lat: Double, 
            lon: Double, 
            photoUri: Uri, 
            alasan: String="",
            onLocationWarning: ((String, Double) -> Unit)? = null,
            onSuccess: (() -> Unit)? = null
        ) {
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
                        fotoMasuk = fotoPart,
                        alasanKeterlambatan = alasan.toRequestBody()
                    )

                    if (response.isSuccessful) {
                        val checkInData = response.body()
                        val photoUrl = checkInData?.fotoMasuk
                        
                        // Update attendance data
                        _todayAttendance.value = _todayAttendance.value.copy(
                            karyawanId = karyawanId,
                            jamMasuk = time,
                            statusMasuk = status,
                            fotoMasuk = photoUrl,
                            lokasiMasukLat = lat,
                            lokasiMasukLong = lon,
                            alasanKeterlambatan = alasan
                        )

                        Log.d("API_SUCCESS", "Check-in berhasil, ID: ${checkInData?.id}")
                        
                        // Check for location warning AFTER saving data
                        checkInData?.locationWarning?.let { warning ->
                            _locationWarning.value = mapOf(
                                "message" to warning.message,
                                "distance_km" to warning.distanceKm,
                                "status" to warning.status
                            )
                            onLocationWarning?.invoke(warning.message, warning.distanceKm)
                        }
                        
                        // Always call success callback if no warning
                        if (checkInData?.locationWarning == null) {
                            onSuccess?.invoke()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("API_CRASH", "Kesalahan jaringan: ${e.message}")
                }
            }
        }

        fun submitCheckOut(time: String, status: String, lat: Double, lon: Double, photoUri: Uri, alasan: String = "") {
            viewModelScope.launch {
                val prefsAuth = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val karyawanId = prefsAuth.getInt("karyawan_id", 0) // Ambil Karyawan ID untuk otorisasi

                // Pengecekan dasar
                if (karyawanId == 0) {
                    Log.e("HomeViewModel", "Fatal: Karyawan ID tidak ditemukan. Tidak dapat checkout.")
                    return@launch
                }
                try {
                    val file = uriToFile(context, photoUri)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val fotoPart = MultipartBody.Part.createFormData("foto_keluar", file.name, requestFile)
                    Log.d("CHECKOUT_DEBUG", "Mencoba mengirim foto dengan nama: ${fotoPart.body.contentType()}")

                    val response = apiService.checkOut(
                        karyawan = karyawanId.toString().toRequestBody(),
                        jamKeluar = time.toRequestBody(),
                        statusKeluar = status.toRequestBody(),
                        lokasiKeluarLat = lat.toString().toRequestBody(),
                        lokasiKeluarLong = lon.toString().toRequestBody(),
                        fotoKeluar = fotoPart,
                        alasanPulangCepat = alasan.toRequestBody()
                    )

                    if (response.isSuccessful) {
                        val attendanceRecord = response.body()
                        val photoUrlKeluar = attendanceRecord?.fotoKeluar

                        _todayAttendance.value = _todayAttendance.value.copy(
                            jamKeluar = time,
                            statusKeluar = status,
                            fotoKeluar = photoUrlKeluar,
                            lokasiKeluarLat = lat,
                            lokasiKeluarLong = lon,
                            alasanPulangCepat = alasan
                        )
                    }else {
                        Log.e("HomeViewModel", "Gagal checkout: ${response.code()} - ${response.errorBody()?.string()}")}
                } catch (e: Exception) {
                    Log.e("API_CRASH", "Kesalahan jaringan: ${e.message}")
                }
            }
        }

        fun submitTimeOffRequest(
            jenisTimeOff: String,
            tanggalMulai: String,
            tanggalSelesai: String,
            alasan: String
        ) {
            viewModelScope.launch {
                try {
                    Log.d("TIMEOFF_REQUEST", "Mengirim: Jenis=$jenisTimeOff, Tanggal=$tanggalMulai")
                    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    val karyawanId = prefs.getInt("karyawan_id", 0)

                    if (karyawanId == 0) {
                        Log.e("HomeViewModel", "Karyawan ID is not set.")
                        return@launch
                    }

                    val response = apiService.timeoff(
                        karyawanId = karyawanId,
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
