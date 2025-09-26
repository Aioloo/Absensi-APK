from rest_framework import serializers
from .models import Karyawan, Absensi, TimeOff
from django.utils import timezone

class KaryawanSerializer(serializers.ModelSerializer):
    sisa_cuti = serializers.SerializerMethodField()
    class Meta:
        model = Karyawan
        fields = ['id', 'nama','perusahaan', 'email', 'divisi','foto_profil', 'jatah_cuti_per_bulan', 'sisa_cuti']

    def get_foto_profil(self, obj):
        if obj.foto_profil:
            request = self.context.get('request')
            return request.build_absolute_uri(obj.foto_profil.url)
        
    def get_sisa_cuti(self, obj):
        cuti_terpakai = TimeOff.objects.filter(
            karyawan=obj,
            jenis='Cuti',
            status__in=['Approved']
        ).count()

        jatah_cuti = obj.jatah_cuti_per_bulan

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
        fields = ['karyawan', 'jam_masuk', 'foto_masuk', 'lokasi_masuk_lat', 'lokasi_masuk_long', 'status_masuk']

    def get_jam_masuk(self, obj):
        return obj.jam_masuk.strftime("%H:%M") if obj.jam_masuk else None

class AbsensiKeluarSerializer(serializers.ModelSerializer):
    class Meta:
        model = Absensi
        fields = ['jam_keluar', 'foto_keluar', 'lokasi_keluar_lat', 'lokasi_keluar_long', 'status_keluar']

    def get_jam_keluar(self, obj):
        return obj.jam_keluar.strftime("%H:%M") if obj.jam_keluar else None
    
class TimeOffSerializer(serializers.ModelSerializer):
    class Meta:
        model = TimeOff
        fields = ['karyawan', 'jenis', 'tanggal_mulai', 'tanggal_selesai', 'alasan', 'status']
        read_only_fields = ['karyawan', 'status']