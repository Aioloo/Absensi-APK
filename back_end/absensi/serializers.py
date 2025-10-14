from rest_framework import serializers
from .models import Karyawan, Absensi, TimeOff, Perusahaan

class PerusahaanSerializer(serializers.ModelSerializer):
    class Meta:
        model = Perusahaan
        fields = ['id', 'nama', 'kode']

class KaryawanSerializer(serializers.ModelSerializer):
    username = serializers.CharField(max_length=150, write_only=True)
    password = serializers.CharField(max_length=128, write_only=True)
    
    # Gunakan SerializerMethodField untuk sisa cuti dan foto profil
    sisa_cuti = serializers.SerializerMethodField()
    foto_profil = serializers.SerializerMethodField()
    sisa_cuti = serializers.SerializerMethodField()
    perusahaan = PerusahaanSerializer(read_only=True)
    foto_profil = serializers.SerializerMethodField()
    
    class Meta:
        model = Karyawan
        fields = ['id', 'nama','perusahaan', 'email', 'divisi','foto_profil', 'jatah_cuti_per_tahun', 'sisa_cuti', 'username', 'password']

    def get_foto_profil(self, obj):
        if obj.foto_profil:
            request = self.context.get('request')
            return request.build_absolute_uri(obj.foto_profil.url)
        return None
        
    def get_sisa_cuti(self, obj):
        cuti_terpakai = TimeOff.objects.filter(
            karyawan=obj,
            jenis='Cuti',
            status__in=['Approved']
        ).count()

        jatah_cuti = obj.jatah_cuti_per_tahun

        return jatah_cuti - cuti_terpakai
class AttendanceListSerializer(serializers.ModelSerializer):
    jam_masuk = serializers.SerializerMethodField()
    jam_keluar = serializers.SerializerMethodField()

    class Meta:
        model = Absensi
        fields = [
            'tanggal',
            'jam_masuk', 'jam_keluar',
            'foto_masuk', 'foto_keluar', 
            'lokasi_masuk_lat', 'lokasi_masuk_long', 'lokasi_keluar_lat', 'lokasi_keluar_long',
            'status_masuk', 'status_keluar']

    def get_jam_masuk(self, obj):
        return obj.jam_masuk.strftime('%H:%M') if obj.jam_masuk else None

    def get_jam_keluar(self, obj):
        return obj.jam_keluar.strftime('%H:%M') if obj.jam_keluar else None

class AbsensiMasukSerializer(serializers.ModelSerializer):
    class Meta:
        model = Absensi
        fields = ['id','karyawan', 'jam_masuk', 'foto_masuk', 'lokasi_masuk_lat', 'lokasi_masuk_long', 'status_masuk','alasan_keterlambatan']

    def get_jam_masuk(self, obj):
        return obj.jam_masuk.strftime("%H:%M") if obj.jam_masuk else None

class AbsensiKeluarSerializer(serializers.ModelSerializer):
    class Meta:
        model = Absensi
        fields = ['id','jam_keluar', 'foto_keluar', 'lokasi_keluar_lat', 'lokasi_keluar_long', 'status_keluar','alasan_pulang_cepat']

    def get_jam_keluar(self, obj):
        return obj.jam_keluar.strftime("%H:%M") if obj.jam_keluar else None
    
class TimeOffSerializer(serializers.ModelSerializer):
    class Meta:
        model = TimeOff
        fields = ['karyawan', 'jenis', 'tanggal_mulai', 'tanggal_selesai', 'alasan', 'status']
        read_only_fields = ['karyawan', 'status']