from django.db import models
from django.contrib.auth.models import User
from django.utils import timezone
from django.core.exceptions import ValidationError

STATUS_IN_CHOICES = [
        ('On Time', 'On Time'),
        ('Telat', 'Telat'),
    ]

STATUS_OUT_CHOICES = [
        ('On Time', 'On Time'),
        ('Pulang Cepat', 'Pulang Cepat'),
    ]

STATUS_LOKASI_CHOICES = [
    ('Di Dalam Area PT PAL', 'Di Dalam Area PT PAL'),
    ('Di Luar Area PT PAL', 'Di Luar Area PT PAL'),
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
        ('Dinas', 'Dinas'),
]

JABATAN_CHOICES = [
    ('direktur', 'Direktur'),
    ('karyawan', 'Karyawan'),
    ('guru', 'Guru'),
]

ROLE_CHOICES = [
    ('superadmin', 'Super Admin'),
    ('direktur', 'Direktur'),
    ('hrd', 'HRD'),
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

# 👤 MODEL ADMIN
class Admin(models.Model):
    username = models.CharField(max_length=150, unique=True, help_text="Username untuk login ke Django Admin")
    email = models.EmailField(unique=True)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, help_text="Role: direktur atau hrd")
    perusahaan = models.ForeignKey(Perusahaan, on_delete=models.CASCADE, 
                                   help_text="Perusahaan yang dikelola")
    user = models.OneToOneField(User, on_delete=models.CASCADE, null=True, blank=True, 
                                related_name='admin_account',
                                help_text="User Django yang terhubung (dibuat otomatis)")
    is_active = models.BooleanField(default=True, help_text="Apakah admin ini aktif")
    
    # TOTP fields
    force_password_change = models.BooleanField(default=True, help_text="User harus ganti password saat login pertama atau setelah reset")
    totp_enabled = models.BooleanField(default=False, help_text="TOTP sudah di-setup atau belum")
    
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        verbose_name = "Admin"
        verbose_name_plural = "Admins"
        ordering = ['username']
    
    def __str__(self):
        role_display = dict(ROLE_CHOICES).get(self.role, self.role)
        return f"{self.username} - {role_display} ({self.perusahaan.nama})"
    
    def clean(self):
        # Validasi: role harus direktur atau hrd saja
        if self.role not in ['direktur', 'hrd']:
            raise ValidationError('Admin hanya bisa memiliki role direktur atau hrd.')
    
    def save(self, *args, **kwargs):
        self.full_clean()
        
        # Jika belum ada user Django, buat otomatis
        if not self.user:
            # Generate password default atau random
            from django.contrib.auth.models import User, Permission
            from django.contrib.contenttypes.models import ContentType
            
            # Cek apakah user dengan username ini sudah ada
            if not User.objects.filter(username=self.username).exists():
                # KEAMANAN: create_user() otomatis melakukan password hashing
                # Password di-hash dengan PBKDF2_SHA256 sebelum disimpan ke database
                user = User.objects.create_user(
                    username=self.username,
                    email=self.email,
                    password='password123',  # ✅ Password ini akan di-hash, BUKAN plain text
                    is_staff=True,
                    is_active=self.is_active
                )
                self.user = user
                
                # Set permissions berdasarkan role
                self._set_user_permissions()
        else:
            # Update user jika ada perubahan (tanpa mengubah password)
            self.user.username = self.username
            self.user.email = self.email
            self.user.is_staff = True
            self.user.is_active = self.is_active
            self.user.save()
            
            # Update permissions jika role berubah
            self._set_user_permissions()
        
        super().save(*args, **kwargs)
    
    def reset_password_to_default(self):
        """
        Reset password ke default 'password123' dan set force_password_change=True
        Digunakan oleh superadmin saat forgot password atau reset both
        """
        if self.user:
            self.user.set_password('password123')
            self.user.save()
            self.force_password_change = True
            self.save(update_fields=['force_password_change'])
    
    def reset_totp(self):
        """
        Reset TOTP dengan menghapus devices dan set totp_enabled=False
        Digunakan oleh superadmin saat forgot TOTP atau reset both
        """
        if self.user:
            # Delete all TOTP devices for this user
            from django_otp.plugins.otp_totp.models import TOTPDevice
            TOTPDevice.objects.filter(user=self.user).delete()
            
            self.totp_enabled = False
            self.save(update_fields=['totp_enabled'])
    
    def _set_user_permissions(self):
        """Set permissions untuk user berdasarkan role"""
        if not self.user:
            return
        
        from django.contrib.auth.models import Permission
        from django.contrib.contenttypes.models import ContentType
        
        # Clear existing permissions
        self.user.user_permissions.clear()
        
        # Get content types untuk models yang relevan
        # Import models di sini untuk menghindari circular import
        from django.apps import apps
        Karyawan = apps.get_model('absensi', 'Karyawan')
        Absensi = apps.get_model('absensi', 'Absensi')
        TimeOff = apps.get_model('absensi', 'TimeOff')
        
        karyawan_ct = ContentType.objects.get_for_model(Karyawan)
        absensi_ct = ContentType.objects.get_for_model(Absensi)
        timeoff_ct = ContentType.objects.get_for_model(TimeOff)
        
        # Permissions yang akan diberikan
        permissions = []
        
        # Direktur dan HRD bisa view, add, change, delete untuk:
        # - Karyawan, Absensi, TimeOff
        for ct in [karyawan_ct, absensi_ct, timeoff_ct]:
            permissions.extend(Permission.objects.filter(content_type=ct))
        
        # HRD juga bisa akses User
        if self.role == 'hrd':
            from django.contrib.auth.models import User as AuthUser
            user_ct = ContentType.objects.get_for_model(AuthUser)
            permissions.extend(Permission.objects.filter(content_type=user_ct))
        
        # Set permissions
        self.user.user_permissions.set(permissions)

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
                raise ValidationError('Anda tidak memiliki sisa cuti untuk tahun ini.')
            
    def save(self, *args, **kwargs):
        self.full_clean()
        super().save(*args, **kwargs)


class Karyawan(models.Model):
        user = models.OneToOneField(User, on_delete=models.CASCADE, null=True, blank=True)
        nama = models.CharField(max_length=255)
        perusahaan = models.ForeignKey(Perusahaan, on_delete=models.CASCADE, related_name='karyawan')
        divisi = models.CharField(max_length=255, null=True, blank=True)
        jabatan = models.CharField(max_length=20, choices=JABATAN_CHOICES, default='karyawan')
        email = models.EmailField(unique=True)
        foto_profil = models.ImageField(upload_to='karyawan_photos/', null=True, blank=True)
        jatah_cuti_per_tahun = models.PositiveIntegerField(default=12)

        def get_sisa_cuti(self):    
            cuti_terpakai_tahun_ini = TimeOff.objects.filter(
                karyawan=self,
                jenis='Cuti',
                status__in=['Approved'],
                created_at__year=timezone.now().date().year
            ).count()
            return self.jatah_cuti_per_tahun - cuti_terpakai_tahun_ini

        def __str__(self):
            return self.nama

class Absensi(models.Model):
        karyawan = models.ForeignKey(Karyawan, on_delete=models.CASCADE)
        tanggal = models.DateField(default=timezone.now)
        alasan_keterlambatan = models.TextField(blank=True, null=True)
        alasan_pulang_cepat = models.TextField(blank=True, null=True)

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
        status_masuk = models.CharField(max_length=20, choices=STATUS_IN_CHOICES, blank=True, null=True)
        status_keluar = models.CharField(max_length=20, choices=STATUS_OUT_CHOICES, blank=True, null=True)
        status_lokasi = models.CharField(max_length=50, choices=STATUS_LOKASI_CHOICES, blank=True, null=True, 
                                         help_text="Status lokasi absensi (di dalam/luar area PT PAL)")

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
