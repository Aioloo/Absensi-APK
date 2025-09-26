from django.db import models
from django.contrib.auth.models import User
from django.utils import timezone
from django.core.exceptions import ValidationError

STATUS_CHOICES = [
        ('On Time', 'On Time'),
        ('Telat', 'Telat'),
    ]

STATUS_TIMEOFF = [
      ('Pending', 'Pending'),
      ('Approved', 'Approved'),
      ('Rejected', 'Rejected'),
]

JENIS_TIMEOFF = [
        ('Cuti', 'Cuti'),
        ('Izin', 'Izin'),
        ('Sakit', 'Sakit'),
]

class TimeOff(models.Model):
    karyawan = models.ForeignKey('Karyawan', on_delete=models.CASCADE, related_name='timeoff')
    jenis = models.CharField(max_length=20, choices=JENIS_TIMEOFF)
    tanggal_mulai = models.DateField()
    tanggal_selesai = models.DateField()
    alasan = models.TextField()
    status = models.CharField(max_length=20, choices=STATUS_TIMEOFF, default='Pending')
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Pengajuan {self.jenis} dari: {self.karyawan.nama} ({self.status})"
    
    def clean(self):
        # Validasi hanya untuk pengajuan 'Cuti'
        if self.jenis == 'Cuti' and self.pk is None:
            sisa_cuti = self.karyawan.get_sisa_cuti()
            if sisa_cuti <= 0:
                raise ValidationError('Anda tidak memiliki sisa cuti untuk bulan ini.')
            
    def save(self, *args, **kwargs):
        self.full_clean()
        super().save(*args, **kwargs)


class Karyawan(models.Model):
        user = models.OneToOneField(User, on_delete=models.CASCADE)
        nama = models.CharField(max_length=255)
        perusahaan = models.CharField(max_length=255, default='PT PAL Indonesia (PERSERO)')
        divisi = models.CharField(max_length=255, null=True, blank=True)
        email = models.EmailField(unique=True)
        foto_profil = models.ImageField(upload_to='karyawan_photos/', null=True, blank=True)
        jatah_cuti_per_bulan = models.PositiveIntegerField(default=3)

        def get_sisa_cuti(self):    
            cuti_terpakai_bulan_ini = TimeOff.objects.filter(
                karyawan=self,
                jenis='Cuti',
                status__in=['Approved'],
                created_at__month=timezone.now().date().month,
                created_at__year=timezone.now().date().year
            ).count()
            
            return self.jatah_cuti_per_bulan - cuti_terpakai_bulan_ini  

        def __str__(self):
            return self.nama

class Absensi(models.Model):
        karyawan = models.ForeignKey(Karyawan, on_delete=models.CASCADE)
        tanggal = models.DateField(auto_now_add=True)

        # waktu
        jam_masuk = models.TimeField(blank=True, null=True)
        jam_keluar = models.TimeField(blank=True, null=True)

        # foto
        foto_masuk = models.ImageField(upload_to='absensi_photos/masuk/', blank=True, null=True)
        foto_keluar = models.ImageField(upload_to='absensi_photos/keluar/', blank=True, null=True)
        
        # lokasi
        lokasi_masuk_lat = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)
        lokasi_masuk_long = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)

        lokasi_keluar_lat = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)
        lokasi_keluar_long = models.DecimalField(max_digits=16, decimal_places=7, blank=True, null=True)

        #status
        status_masuk = models.CharField(max_length=20, choices=STATUS_CHOICES, blank=True, null=True)
        status_keluar = models.CharField(max_length=20, choices=STATUS_CHOICES, blank=True, null=True)

        def __str__(self):
            return f"Absensi {self.karyawan.nama} pada {self.tanggal}"
