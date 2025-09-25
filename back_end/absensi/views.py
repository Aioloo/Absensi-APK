from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, IsAdminUser
from rest_framework.views import APIView
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import authenticate
from .models import Karyawan, Absensi, TimeOff
from .serializers import (
    KaryawanSerializer, AbsensiMasukSerializer, AbsensiKeluarSerializer, AttendanceListSerializer, TimeOffSerializer
)
from datetime import date, datetime, time, timezone
from django.http import HttpResponse

def home_view(request):
    return HttpResponse("Welcome to the Employee Attendance System")

class TimeOffViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated]

    @action(detail=False, methods=['post'])
    def ajukan(self, request):
        karyawan = Karyawan.objects.get(user=request.user)

        if request.data.get('jenis') == 'Cuti':
             sisa_cuti = karyawan.get_sisa_cuti()
             if sisa_cuti <= 0:
                 return Response({'error': 'Anda tidak memiliki sisa cuti untuk bulan ini.'}, status=status.HTTP_400_BAD_REQUEST)

        serializer = TimeOffSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save(karyawan=karyawan)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    
    def list(self, request):
        queryset = TimeOff.objects.all().order_by('-created_at')
        serializer = TimeOffSerializer(queryset, many=True)
        return Response(serializer.data)

    @action(detail=False, methods=['get'])
    def history(self, request):
        karyawan = Karyawan.objects.get(user=request.user)
        queryset = TimeOff.objects.filter(karyawan=karyawan).order_by('-created_at')
        serializer = TimeOffSerializer(queryset, many=True)
        return Response(serializer.data)
    
    @action(detail=True, methods=['post'], permission_classes=[IsAdminUser])
    def approve(self, request, pk=None):
        try:
            timeoff_request = TimeOff.objects.get(pk=pk)
        except TimeOff.DoesNotExist:
            return Response({"detail": "Pengajuan tidak ditemukan."}, status=status.HTTP_404_NOT_FOUND)

        if timeoff_request.status == 'APPROVED':
            return Response({"detail": "Pengajuan ini sudah disetujui."}, status=status.HTTP_400_BAD_REQUEST)

        # Ubah status menjadi 'APPROVED' dan simpan
        timeoff_request.status = 'APPROVED'
        timeoff_request.save()

        serializer = TimeOffSerializer(timeoff_request)
        return Response(serializer.data, status=status.HTTP_200_OK)
    
class KaryawanViewSet(viewsets.ModelViewSet):
    queryset = Karyawan.objects.all()
    serializer_class = KaryawanSerializer
    permission_classes = [IsAuthenticated]

class AbsensiViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated]

    @action(detail=False, methods=['get'])
    def counts (self, request):
        karyawan = Karyawan.objects.get(user = request.user)
        
        total_kehadiran = Absensi.objects.filter(karyawan = karyawan).count()

        data = {
            'total_kehadiran' : total_kehadiran
        }

        return Response(data)

    @action(detail=False, methods=['get'])
    def history(self, request):
        karyawan = Karyawan.objects.get(user=request.user)
        queryset = Absensi.objects.filter(karyawan=karyawan).order_by('-tanggal')
        serializer = AttendanceListSerializer(queryset, many=True)
        return Response(serializer.data)
    
    def list(self, request):
        return self.history
    
    @action(detail=False, methods=['post'], parser_classes=[MultiPartParser, FormParser])
    def checkin(self, request):
        karyawan = Karyawan.objects.get(user=request.user)
        today = date.today()
        absensi_sudah_ada = Absensi.objects.filter(karyawan=karyawan, tanggal=today).exists()
        
        if absensi_sudah_ada:
            return Response({"detail": "Anda sudah absen masuk hari ini."}, status=status.HTTP_400_BAD_REQUEST)
        
        data = request.data
        data['karyawan'] = karyawan.id
        
        jam_masuk_str = data.get('jam_masuk')
        if not jam_masuk_str:
            return Response({"error": "Waktu masuk tidak disertakan."}, status=status.HTTP_400_BAD_REQUEST)

        try:
            jam_masuk_obj = datetime.strptime(jam_masuk_str, '%H:%M').time()
        except ValueError:
            return Response({"error": "Format waktu masuk tidak valid. Gunakan HH:MM."}, status=status.HTTP_400_BAD_REQUEST)

        checkin_limit = time(7, 30)
        status_absensi = 'On Time' if jam_masuk_obj <= checkin_limit else 'Telat'

        data['status_masuk'] = status_absensi
        data['jam_masuk'] = jam_masuk_obj

        serializer = AbsensiMasukSerializer(data=data)
        serializer.is_valid(raise_exception=True)
        serializer.save(karyawan=karyawan)
        return Response(serializer.data, status=status.HTTP_201_CREATED)

    @action(detail=True, methods=['patch'], parser_classes=[MultiPartParser, FormParser])
    def checkout(self, request, pk=None):
        try:
            absensi = Absensi.objects.get(pk=pk, karyawan__user=request.user)
        except Absensi.DoesNotExist:
            return Response({"detail": "Record absensi tidak ditemukan."}, status=status.HTTP_404_NOT_FOUND)
        
        if absensi.jam_keluar:
            return Response({"detail": "Anda sudah absen keluar hari ini."}, status=status.HTTP_400_BAD_REQUEST)

        serializer = AbsensiKeluarSerializer(absensi, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)
    
class LoginView(APIView):
    def post(self, request):
        username = request.data.get('username')
        password = request.data.get('password')

        user = authenticate(username=username, password=password)

        if user:
            # Login berhasil, kirim token
            refresh = RefreshToken.for_user(user)
            return Response({
                'token': str(refresh.access_token),
                'karyawan_id': user.karyawan.id
            }, status=status.HTTP_200_OK)
        else:
            # Login gagal
            return Response({'detail': 'Kredensial tidak valid.'}, status=status.HTTP_401_UNAUTHORIZED)