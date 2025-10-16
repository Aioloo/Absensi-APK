from django.db import transaction
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
                 return Response({'error': 'Anda tidak memiliki sisa cuti untuk tahun ini.'}, status=status.HTTP_400_BAD_REQUEST)

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
    serializer_class = KaryawanSerializer
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        # Filter karyawan berdasarkan perusahaan yang sama dengan user yang login
        user_karyawan = Karyawan.objects.get(user=self.request.user)
        return Karyawan.objects.filter(perusahaan=user_karyawan.perusahaan)
    
    @transaction.atomic
    def create(self, request, *args, **kwargs):
        # Tambahkan pemeriksaan izin admin di sini jika permission_classes adalah IsAuthenticated
        if not self.request.user.is_staff:
             return Response({'detail': 'Anda tidak memiliki izin untuk melakukan operasi ini.'}, status=status.HTTP_403_FORBIDDEN)

        # 1. Validasi data
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # 2. Ambil data User (username dan password)
        username = serializer.validated_data.pop('username')
        password = serializer.validated_data.pop('password')
        
        # 3. Buat User baru
        try:
            user = User.objects.create_user(username=username, password=password)
        except Exception as e:
            return Response({'error': 'Gagal membuat akun user. Username mungkin sudah ada.'}, status=status.HTTP_400_BAD_REQUEST)

        # 4. Buat objek Karyawan dan kaitkan dengan User
        karyawan = Karyawan.objects.create(user=user, **serializer.validated_data)
        
        # 5. Kirim respons
        response_serializer = KaryawanSerializer(karyawan, context={'request': request}) 
        
        return Response(response_serializer.data, status=status.HTTP_201_CREATED)

class AbsensiViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated]

    @action(detail=False, methods=['get'])
    def counts (self, request):
        karyawan = Karyawan.objects.get(user = request.user)
        
        total_kehadiran = Absensi.objects.filter(karyawan = karyawan).count()
        total_timeoff = TimeOff.objects.filter(karyawan = karyawan, status='Approved').count()

        data = {
            'total_kehadiran' : total_kehadiran,
            'total_timeoff' : total_timeoff
        }

        return Response(data)

    @action(detail=False, methods=['get'])
    def history(self, request):
        karyawan = Karyawan.objects.get(user=request.user)
        queryset = Absensi.objects.filter(karyawan=karyawan).order_by('-tanggal')
        serializer = AttendanceListSerializer(queryset, many=True)
        return Response(serializer.data)
    
    def list(self, request):
        return self.history(request)
    
    @action(detail=False, methods=['post'], parser_classes=[MultiPartParser, FormParser])
    def checkin(self, request):
        from django.utils import timezone as django_timezone
        import pytz
        
        karyawan = Karyawan.objects.get(user=request.user)
        today = date.today()
        absensi_sudah_ada = Absensi.objects.filter(karyawan=karyawan, tanggal=today).exists()
        
        if absensi_sudah_ada:
            return Response({"detail": "Anda sudah absen masuk hari ini."}, status=status.HTTP_400_BAD_REQUEST)
        
        # KEAMANAN: Gunakan waktu SERVER, bukan waktu client
        jakarta_tz = pytz.timezone('Asia/Jakarta')
        server_time_now = django_timezone.now().astimezone(jakarta_tz)
        server_jam_masuk = server_time_now.time()
        server_tanggal = server_time_now.date()
        
        # Ambil waktu dari client untuk validasi (opsional)
        client_jam_masuk_str = request.data.get('jam_masuk')
        client_jam_masuk = None
        
        if client_jam_masuk_str:
            try:
                client_jam_masuk = datetime.strptime(client_jam_masuk_str, '%H:%M').time()
            except ValueError:
                pass
        
        # VALIDASI ANTI-MANIPULASI: Bandingkan waktu client vs server
        time_diff_minutes = 0
        if client_jam_masuk:
            client_minutes = client_jam_masuk.hour * 60 + client_jam_masuk.minute
            server_minutes = server_jam_masuk.hour * 60 + server_jam_masuk.minute
            time_diff_minutes = abs(server_minutes - client_minutes)
            
            # BUAT LOG AUDIT UNTUK SEMUA PERCOBAAN
            from .models import SecurityAuditLog
            severity = 'LOW'
            if time_diff_minutes > 30:
                severity = 'CRITICAL'
            elif time_diff_minutes > 15:
                severity = 'HIGH'
            elif time_diff_minutes > 10:
                severity = 'MEDIUM'
            
            # Log audit
            SecurityAuditLog.objects.create(
                karyawan=karyawan,
                audit_type='TIME_MANIPULATION',
                description=f'Percobaan check-in dengan selisih waktu {time_diff_minutes} menit. Client: {client_jam_masuk}, Server: {server_jam_masuk}',
                client_time=client_jam_masuk,
                server_time=server_jam_masuk,
                time_difference_minutes=time_diff_minutes,
                severity=severity,
                user_agent=request.META.get('HTTP_USER_AGENT', ''),
                ip_address=request.META.get('REMOTE_ADDR', '')
            )
            
            # Jika selisih > 10 menit, curigai manipulasi
            if time_diff_minutes > 10:
                return Response({
                    "error": "Terdeteksi ketidaksesuaian waktu sistem. Pastikan waktu HP Anda sinkron dengan server.",
                    "server_time": server_jam_masuk.strftime('%H:%M'),
                    "client_time": client_jam_masuk.strftime('%H:%M'),
                    "warning": "Sistem menggunakan waktu server untuk keamanan.",
                    "security_alert": "Aktivitas ini telah dicatat dalam log audit keamanan."
                }, status=status.HTTP_400_BAD_REQUEST)
        
        # GUNAKAN WAKTU SERVER (ANTI-MANIPULASI)
        checkin_limit = time(7, 30)
        status_absensi = 'On Time' if server_jam_masuk <= checkin_limit else 'Telat'
        
        # Siapkan data dengan waktu server
        data = request.data
        data['karyawan'] = karyawan.id
        data['jam_masuk'] = server_jam_masuk
        data['status_masuk'] = status_absensi
        data['tanggal'] = server_tanggal  # Force server date

        if status_absensi == 'Telat' and not data.get('alasan_keterlambatan'):
            return Response({"error": "Alasan keterlambatan wajib diisi jika Anda terlambat."}, status=status.HTTP_400_BAD_REQUEST)
        
        serializer = AbsensiMasukSerializer(data=data)
        serializer.is_valid(raise_exception=True)
        absensi = serializer.save(karyawan=karyawan)
        
        # Return response dengan waktu server
        response_data = serializer.data
        response_data['server_time'] = server_jam_masuk.strftime('%H:%M')
        response_data['security_note'] = 'Waktu diverifikasi dengan server untuk keamanan'
        
        return Response(response_data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=['post'], parser_classes=[MultiPartParser, FormParser])
    def checkout(self, request):
        from django.utils import timezone as django_timezone

        karyawan = Karyawan.objects.get(user=request.user)

        try:
            absensi = Absensi.objects.get(karyawan=karyawan, tanggal=date.today())
        except Absensi.DoesNotExist:
            return Response({"detail": "Record absensi tidak ditemukan."}, status=status.HTTP_404_NOT_FOUND)
        
        if absensi.jam_keluar:
            return Response({"detail": "Anda sudah absen keluar hari ini."}, status=status.HTTP_400_BAD_REQUEST)

        # KEAMANAN: Gunakan waktu SERVER untuk checkout
        try:
            import pytz
            jakarta_tz = pytz.timezone('Asia/Jakarta')
            server_time_now = django_timezone.now().astimezone(jakarta_tz)
        except ImportError:
            # Fallback jika pytz tidak tersedia
            server_time_now = django_timezone.now()
        
        server_jam_keluar = server_time_now.time()
        
        # Ambil waktu dari client untuk validasi
        client_jam_keluar_str = request.data.get('jam_keluar')
        client_jam_keluar = None
        
        if client_jam_keluar_str:
            try:
                from datetime import datetime
                client_jam_keluar = datetime.strptime(client_jam_keluar_str, '%H:%M').time()
            except ValueError:
                pass
        
        # VALIDASI ANTI-MANIPULASI
        time_diff_minutes = 0
        if client_jam_keluar:
            client_minutes = client_jam_keluar.hour * 60 + client_jam_keluar.minute
            server_minutes = server_jam_keluar.hour * 60 + server_jam_keluar.minute
            time_diff_minutes = abs(server_minutes - client_minutes)
            
            # BUAT LOG AUDIT CHECKOUT
            from .models import SecurityAuditLog
            severity = 'LOW'
            if time_diff_minutes > 30:
                severity = 'CRITICAL'
            elif time_diff_minutes > 15:
                severity = 'HIGH'
            elif time_diff_minutes > 10:
                severity = 'MEDIUM'
            
            # Log audit checkout
            SecurityAuditLog.objects.create(
                karyawan=absensi.karyawan,
                audit_type='TIME_MANIPULATION',
                description=f'Percobaan check-out dengan selisih waktu {time_diff_minutes} menit. Client: {client_jam_keluar}, Server: {server_jam_keluar}',
                client_time=client_jam_keluar,
                server_time=server_jam_keluar,
                time_difference_minutes=time_diff_minutes,
                severity=severity,
                user_agent=request.META.get('HTTP_USER_AGENT', ''),
                ip_address=request.META.get('REMOTE_ADDR', '')
            )
            
            # Jika selisih > 10 menit, curigai manipulasi
            if time_diff_minutes > 10:
                return Response({
                    "error": "Terdeteksi ketidaksesuaian waktu sistem saat checkout.",
                    "server_time": server_jam_keluar.strftime('%H:%M'),
                    "client_time": client_jam_keluar.strftime('%H:%M'),
                    "warning": "Sistem menggunakan waktu server untuk keamanan.",
                    "security_alert": "Aktivitas ini telah dicatat dalam log audit keamanan."
                }, status=status.HTTP_400_BAD_REQUEST)
        
        # FORCE GUNAKAN WAKTU SERVER 
        from datetime import time
        checkout_time = time(16, 30)
        status_keluar = 'Pulang Cepat' if server_jam_keluar < checkout_time else 'On Time'

        alasan_pulang_cepat = request.data.get('alasan_pulang_cepat', '')

        if status_keluar == 'Pulang Cepat' and not alasan_pulang_cepat:
            return Response({"error": "Alasan pulang cepat wajib diisi jika Anda pulang sebelum jam 16:30."}, status=status.HTTP_400_BAD_REQUEST)
        
        # Update data dengan waktu server
        data = request.data.copy()
        data['jam_keluar'] = server_jam_keluar.strftime('%H:%M')
        data['status_keluar'] = status_keluar
        data['alasan_pulang_cepat'] = alasan_pulang_cepat
        
        serializer = AbsensiKeluarSerializer(absensi, data=data, partial=True)
        serializer.is_valid(raise_exception=True)
        absensi_updated = serializer.save()
        
        # Response dengan info keamanan
        response_data = serializer.data
        response_data['server_time'] = server_jam_keluar.strftime('%H:%M')
        response_data['security_note'] = 'Waktu checkout diverifikasi dengan server'
        
        return Response(response_data, status=status.HTTP_200_OK)
    
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