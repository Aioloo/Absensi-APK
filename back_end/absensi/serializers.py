from rest_framework import serializers
from .models import Karyawan, Absensi, Cuti

class KaryawanSerializer(serializers.ModelSerializer):
    class Meta:
        model = Karyawan
        fields = ['id', 'nama', 'email', 'divisi','foto_profil']

    def get_foto_profil(self, obj):
        if obj.foto_profil:
            request = self.context.get('request')
            return request.build_absolute_uri(obj.foto_profil.url)
        return None

class AttendanceListSerializer(serializers.ModelSerializer):
    jam_masuk = serializers.SerializerMethodField()
    jam_keluar = serializers.SerializerMethodField()
    jam_absen = serializers.SerializerMethodField()

    class Meta:
        model = Absensi
        fields = [
            'tanggal',
            'jam_masuk', 'jam_keluar', 'jam_absen', 
            'foto_masuk', 'foto_keluar', 'foto_absen', 
            'lokasi_masuk_lat', 'lokasi_masuk_long', 'lokasi_keluar_lat', 'lokasi_keluar_long', 'lokasi_absen_lat', 'lokasi_absen_long', 
            'status_masuk', 'status_keluar', 'status_absen']

    def get_jam_masuk(self, obj):
        return obj.jam_masuk.strftime('%H:%M') if obj.jam_masuk else None

    def get_jam_keluar(self, obj):
        return obj.jam_keluar.strftime('%H:%M') if obj.jam_keluar else None

    def get_jam_absen(self, obj):
        return obj.jam_absen.strftime('%H:%M') if obj.jam_absen else None

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

class AbsensiAbsenSerializer(serializers.ModelSerializer):
    class Meta:
        model = Absensi
        fields = ['karyawan', 'jam_absen', 'foto_absen', 'lokasi_absen_lat', 'lokasi_absen_long', 'status_absen']

    def get_jam_absen(self, obj):
        return obj.jam_absen.strftime("%H:%M") if obj.jam_absen else None


class CutiSerializer(serializers.ModelSerializer):
    class Meta:
        model = Cuti
        fields = '__all__'
        read_only_fields = ['karyawan', 'status']