from django.db import models
from django.contrib.auth.models import User

STATUS_CHOICES = [
        ('On Time', 'On Time'),
        ('Telat', 'Telat'),
        ('Absen', 'Absen'),
    ]

STATUS_CUTI = [
      ('Pending', 'Pending'),
      ('Approved', 'Disetujui'),
      ('Rejected', 'Ditolak'),
]

class Cuti(models.Model):
        karyawan = models.ForeignKey('Karyawan', on_delete=models.CASCADE)
        tanggal_mulai = models.DateField()
        tanggal_selesai = models.DateField()
        alasan = models.TextField()
        status = models.CharField(max_length=20, choices=STATUS_CUTI, default='Pending')
        created_at = models.DateTimeField(auto_now_add=True)

        def __str__(self):
            return f"Pengajuan cuti dari : {self.karyawan.nama} ({self.status})"
        

class Karyawan(models.Model):
        user = models.OneToOneField(User, on_delete=models.CASCADE)
        nama = models.CharField(max_length=255)
        divisi = models.CharField(max_length=255, null=True, blank=True)
        email = models.EmailField(unique=True)
        foto_profil = models.ImageField(upload_to='karyawan_photos/', null=True, blank=True)

        def __str__(self):
            return self.nama

class Absensi(models.Model):
        karyawan = models.ForeignKey(Karyawan, on_delete=models.CASCADE)
        tanggal = models.DateField(auto_now_add=True)

        # waktu
        jam_masuk = models.TimeField(blank=True, null=True)
        jam_keluar = models.TimeField(blank=True, null=True)
        jam_absen = models.TimeField(blank=True, null=True)

        # foto
        foto_masuk = models.ImageField(upload_to='absensi_photos/masuk/', blank=True, null=True)
        foto_keluar = models.ImageField(upload_to='absensi_photos/keluar/', blank=True, null=True)
        foto_absen = models.ImageField(upload_to='absensi_photos/absen/', blank=True, null=True)
        
        # lokasi
        lokasi_masuk_lat = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)
        lokasi_masuk_long = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)

        lokasi_keluar_lat = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)
        lokasi_keluar_long = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)

        lokasi_absen_lat = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)
        lokasi_absen_long = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)

        #status
        status_masuk = models.CharField(max_length=20, choices=STATUS_CHOICES, blank=True, null=True)
        status_keluar = models.CharField(max_length=20, choices=STATUS_CHOICES, blank=True, null=True)
        status_absen = models.CharField(max_length=20, choices=STATUS_CHOICES, blank=True, null=True)

        def __str__(self):
            return f"Absensi {self.karyawan.nama} pada {self.tanggal}"
