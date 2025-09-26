from django.contrib import admin
from django.contrib.auth.models import Group
from .models import Karyawan, Absensi, TimeOff # <-- Pastikan semua model diimpor

# Unregister Groups model karena tidak digunakan
admin.site.unregister(Group)

# Kustomisasi model Karyawan
@admin.register(Karyawan)
class KaryawanAdmin(admin.ModelAdmin):
    # Field yang akan ditampilkan di halaman daftar
    list_display = ('nama', 'perusahaan', 'divisi', 'email')
    # Field yang bisa dicari
    search_fields = ('nama', 'perusahaan', 'divisi', 'email')
    # Filter berdasarkan perusahaan
    list_filter = ('perusahaan', 'divisi')

# Kustomisasi model Absensi
@admin.register(Absensi)
class AbsensiAdmin(admin.ModelAdmin):
    list_display = ('karyawan', 'tanggal', 'jam_masuk', 'jam_keluar', 'status_masuk')
    list_filter = ('tanggal', 'status_masuk')
    search_fields = ('karyawan__nama',)

# Kustomisasi model TimeOff
@admin.register(TimeOff)
class TimeOffAdmin(admin.ModelAdmin):
    list_display = ('karyawan', 'jenis', 'tanggal_mulai', 'status')
    list_filter = ('jenis', 'status')
    search_fields = ('karyawan__nama',)
    actions = ['approve_requests', 'reject_requests']
    
    @admin.action(description="Setujui pengajuan yang dipilih")
    def approve_requests(self, request, queryset):
        queryset.update(status='Approved')
        self.message_user(request, f"{queryset.count()} pengajuan telah disetujui.")
    
    @admin.action(description="Tolak pengajuan yang dipilih")
    def reject_requests(self, request, queryset):
        queryset.update(status='Rejected')
        self.message_user(request, f"{queryset.count()} pengajuan telah ditolak.")