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

class Perusahaan(models.Model):
    nama = models.CharField(max_length=255, unique=True)
    kode = models.CharField(max_length=10, unique=True, help_text="Kode singkat perusahaan (contoh: PAL)")
    alamat = models.TextField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "Perusahaan"
        verbose_name_plural = "Perusahaan"
        ordering = ['nama']

    def __str__(self):
        return self.nama

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
        perusahaan = models.ForeignKey(Perusahaan, on_delete=models.CASCADE, related_name='karyawan')
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
        tanggal = models.DateField(default=timezone.now)

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


# 🔐 MODEL AUDIT KEAMANAN
class SecurityAuditLog(models.Model):
    AUDIT_TYPES = [
        ('TIME_MANIPULATION', 'Manipulasi Waktu'),
        ('LOCATION_MISMATCH', 'Lokasi Tidak Sesuai'),
        ('SUSPICIOUS_LOGIN', 'Login Mencurigakan'),
        ('DUPLICATE_ATTEMPT', 'Percobaan Duplikat'),
        ('SYSTEM_VALIDATION', 'Validasi Sistem'),
    ]
    
    karyawan = models.ForeignKey(Karyawan, on_delete=models.CASCADE)
    audit_type = models.CharField(max_length=30, choices=AUDIT_TYPES)
    description = models.TextField()
    
    # Time details
    client_time = models.TimeField(null=True, blank=True)
    server_time = models.TimeField(null=True, blank=True)
    time_difference_minutes = models.IntegerField(null=True, blank=True)
    
    # Location details  
    client_lat = models.DecimalField(max_digits=16, decimal_places=7, null=True, blank=True)
    client_lng = models.DecimalField(max_digits=16, decimal_places=7, null=True, blank=True)
    
    # System info
    user_agent = models.TextField(null=True, blank=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    
    # Severity level
    SEVERITY_CHOICES = [
        ('LOW', 'Rendah'),
        ('MEDIUM', 'Menengah'), 
        ('HIGH', 'Tinggi'),
        ('CRITICAL', 'Kritis'),
    ]
    severity = models.CharField(max_length=10, choices=SEVERITY_CHOICES, default='MEDIUM')
    
    # Status handling
    STATUS_CHOICES = [
        ('PENDING', 'Menunggu Review'),
        ('REVIEWED', 'Sudah Direview'),
        ('RESOLVED', 'Teratasi'),
        ('FALSE_POSITIVE', 'False Positive'),
    ]
    status = models.CharField(max_length=15, choices=STATUS_CHOICES, default='PENDING')
    
    created_at = models.DateTimeField(auto_now_add=True)
    reviewed_by = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='reviewed_audits')
    notes = models.TextField(null=True, blank=True)
    
    class Meta:
        ordering = ['-created_at']
        verbose_name = "Log Audit Keamanan"
        verbose_name_plural = "Log Audit Keamanan"
    
    def __str__(self):
        return f"{self.audit_type} - {self.karyawan.nama} ({self.created_at.strftime('%d/%m/%Y %H:%M')})"
