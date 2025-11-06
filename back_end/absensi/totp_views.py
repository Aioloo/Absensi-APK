"""
TOTP (Two-Factor Authentication) Views
Handles TOTP setup, verification, and authentication flow
"""

import io
import qrcode
from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required
from django.contrib.auth import login, authenticate
from django.contrib import messages
from django.http import HttpResponse
from django_otp.plugins.otp_totp.models import TOTPDevice
from django_otp.util import random_hex
from django.views.decorators.http import require_http_methods
from django.contrib.auth.views import PasswordChangeView
from django.urls import reverse_lazy


class CustomPasswordChangeView(PasswordChangeView):
    """
    Override default password change view to set force_password_change=False
    setelah user berhasil ganti password
    """
    success_url = reverse_lazy('totp_check_setup')
    template_name = 'admin/password_change_form.html'
    
    def form_valid(self, form):
        response = super().form_valid(form)
        
        # Set force_password_change=False setelah password berhasil diubah
        if hasattr(self.request.user, 'admin_account'):
            admin = self.request.user.admin_account
            admin.force_password_change = False
            admin.save(update_fields=['force_password_change'])
            messages.success(self.request, 'Password berhasil diubah!')
        
        return response


@login_required
def totp_check_setup(request):
    """
    Check apakah user sudah setup TOTP atau belum
    Jika belum, redirect ke setup page
    Jika sudah, redirect ke admin dashboard
    """
    if not hasattr(request.user, 'admin_account'):
        # Bukan admin account, redirect ke admin
        return redirect('/admin/')
    
    admin = request.user.admin_account
    
    # Check jika TOTP sudah di-setup
    if admin.totp_enabled:
        return redirect('/admin/')
    
    # Jika belum, redirect ke setup page
    return redirect('totp_setup')


@login_required
def totp_setup(request):
    """
    TOTP Setup Page - Generate QR code untuk first-time setup
    """
    if not hasattr(request.user, 'admin_account'):
        messages.error(request, 'Anda tidak memiliki akses ke halaman ini.')
        return redirect('/admin/')
    
    admin = request.user.admin_account
    
    # Jika TOTP sudah di-setup, redirect ke admin
    if admin.totp_enabled:
        messages.info(request, 'TOTP sudah di-setup sebelumnya.')
        return redirect('/admin/')
    
    # Hapus semua unconfirmed devices lama untuk user ini
    # Ini mencegah multiple QR codes di Google Authenticator
    TOTPDevice.objects.filter(user=request.user, confirmed=False).delete()
    
    # Buat device baru yang unik
    device = TOTPDevice.objects.create(
        user=request.user,
        name='default',
        confirmed=False
    )
    
    # Generate OTP URI for QR code
    otp_uri = device.config_url
    
    context = {
        'otp_uri': otp_uri,
        'device': device,
        'admin': admin,
    }
    
    return render(request, 'totp/totp_setup.html', context)


@login_required
def totp_qr_code(request):
    """
    Generate QR code image untuk TOTP setup
    """
    if not hasattr(request.user, 'admin_account'):
        return HttpResponse('Unauthorized', status=401)
    
    # Get unconfirmed device
    device = TOTPDevice.objects.filter(user=request.user, confirmed=False).first()
    
    if not device:
        return HttpResponse('No device found', status=404)
    
    # Generate QR code
    otp_uri = device.config_url
    qr = qrcode.QRCode(version=1, box_size=10, border=5)
    qr.add_data(otp_uri)
    qr.make(fit=True)
    
    img = qr.make_image(fill_color="black", back_color="white")
    
    # Convert to bytes
    buffer = io.BytesIO()
    img.save(buffer, format='PNG')
    buffer.seek(0)
    
    return HttpResponse(buffer, content_type='image/png')


@login_required
@require_http_methods(["POST"])
def totp_verify_setup(request):
    """
    Verify TOTP code during setup
    """
    if not hasattr(request.user, 'admin_account'):
        messages.error(request, 'Unauthorized')
        return redirect('/admin/')
    
    admin = request.user.admin_account
    token = request.POST.get('token', '').strip()
    
    if not token:
        messages.error(request, 'Kode TOTP tidak boleh kosong.')
        return redirect('totp_setup')
    
    # Get unconfirmed device
    device = TOTPDevice.objects.filter(user=request.user, confirmed=False).first()
    
    if not device:
        messages.error(request, 'Device tidak ditemukan. Silakan setup ulang.')
        return redirect('totp_setup')
    
    # Verify token
    if device.verify_token(token):
        # Hapus semua unconfirmed devices lainnya (jika ada)
        TOTPDevice.objects.filter(user=request.user, confirmed=False).exclude(id=device.id).delete()
        
        # Confirm device ini
        device.confirmed = True
        device.save()
        
        # Set totp_enabled=True di Admin model
        admin.totp_enabled = True
        admin.save(update_fields=['totp_enabled'])
        
        # Generate backup codes (TIDAK disimpan di database untuk keamanan)
        # Backup codes hanya ditampilkan 1x ke user, setelah itu hilang
        # Jika user kehilangan backup codes DAN phone, harus reset TOTP by superadmin
        backup_codes = []
        for _ in range(10):
            token = random_hex(length=8)
            backup_codes.append(token)
        
        # Store backup codes in session HANYA untuk display (temporary)
        # Setelah user acknowledge, backup codes hilang dari session
        request.session['backup_codes'] = backup_codes
        
        messages.success(request, 'TOTP berhasil di-setup! PENTING: Simpan backup codes di bawah ini, tidak akan ditampilkan lagi!')
        return redirect('totp_backup_codes')
    else:
        messages.error(request, 'Kode TOTP salah. Silakan coba lagi.')
        return redirect('totp_setup')


@login_required
def totp_backup_codes(request):
    """
    Display backup codes setelah TOTP setup
    """
    backup_codes = request.session.get('backup_codes', [])
    
    if not backup_codes:
        messages.info(request, 'Backup codes sudah ditampilkan sebelumnya.')
        return redirect('/admin/')
    
    context = {
        'backup_codes': backup_codes,
    }
    
    return render(request, 'totp/totp_backup_codes.html', context)


@login_required
@require_http_methods(["POST"])
def totp_backup_codes_acknowledge(request):
    """
    User mengakui sudah menyimpan backup codes
    """
    # Clear backup codes from session
    if 'backup_codes' in request.session:
        del request.session['backup_codes']
    
    messages.success(request, 'TOTP setup selesai! Anda sekarang bisa menggunakan admin panel.')
    return redirect('/admin/')


@login_required
def totp_verify(request):
    """
    Verify TOTP code setelah login dengan password
    """
    if not hasattr(request.user, 'admin_account'):
        return redirect('/admin/')
    
    admin = request.user.admin_account
    
    # Jika TOTP belum di-setup, redirect ke setup
    if not admin.totp_enabled:
        return redirect('totp_setup')
    
    # Jika user sudah verified (OTP), redirect ke admin
    if request.user.is_verified():
        return redirect('/admin/')
    
    if request.method == 'POST':
        token = request.POST.get('token', '').strip()
        
        if not token:
            messages.error(request, 'Kode TOTP tidak boleh kosong.')
            return render(request, 'totp/totp_verify.html')
        
        # Verify with TOTP device only (NO backup codes)
        device = TOTPDevice.objects.filter(user=request.user, confirmed=True).first()
        
        if device and device.verify_token(token):
            # Mark user as verified for this session
            from django_otp import login as otp_login
            otp_login(request, device)
            
            messages.success(request, 'TOTP verified! Selamat datang.')
            return redirect('/admin/')
        
        messages.error(request, 'Kode TOTP salah. Silakan coba lagi.')
    
    return render(request, 'totp/totp_verify.html')
