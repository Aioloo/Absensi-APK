# TOTP (Two-Factor Authentication) Implementation Guide

## 📋 Overview

TOTP (Time-based One-Time Password) authentication telah berhasil diimplementasikan untuk role **HRD** dan **Direktur** di Absensi-APK system.

## ✅ Komponen yang Sudah Diimplementasikan

### 1. **Database Models** (`absensi/models.py`)
- ✅ Field `force_password_change` (Boolean) - untuk memaksa user ganti password
- ✅ Field `totp_enabled` (Boolean) - track status TOTP setup
- ✅ Method `reset_password_to_default()` - reset password ke 'password123'
- ✅ Method `reset_totp()` - delete TOTP devices dan set totp_enabled=False

### 2. **Password Validator** (`absensi/validators.py`)
Password requirements:
- ✅ Minimum 9 karakter
- ✅ Minimal 1 huruf besar (A-Z)
- ✅ Minimal 1 huruf kecil (a-z)
- ✅ Minimal 1 simbol (!@#$%^&* dll)
- ✅ Tidak boleh ada spasi

### 3. **Middleware** (`absensi/middleware.py`)
- ✅ `ContentSecurityPolicyMiddleware` - CSP headers untuk keamanan
- ✅ `ForcePasswordChangeMiddleware` - redirect ke password change jika `force_password_change=True`
- ✅ Auto-redirect ke TOTP setup jika `totp_enabled=False`
- ✅ Auto-redirect ke TOTP verify jika TOTP belum verified di session

### 4. **Admin Actions** (`absensi/admin.py`)
Superadmin dapat melakukan:
- ✅ **Reset Password** - set password ke default dan force password change
- ✅ **Reset TOTP** - delete TOTP devices (forgot TOTP scenario)
- ✅ **Reset Both** - reset password DAN TOTP sekaligus
- ✅ TOTP status indicator dengan warna di admin list

### 5. **Views** (`absensi/totp_views.py`)
- ✅ `CustomPasswordChangeView` - override password change untuk set force_password_change=False
- ✅ `totp_check_setup` - check apakah TOTP sudah di-setup
- ✅ `totp_setup` - halaman setup TOTP dengan QR code
- ✅ `totp_qr_code` - generate QR code image
- ✅ `totp_verify_setup` - verify TOTP code saat setup
- ✅ `totp_backup_codes` - display backup codes setelah setup
- ✅ `totp_backup_codes_acknowledge` - user acknowledge sudah simpan backup codes
- ✅ `totp_verify` - verify TOTP code saat login

### 6. **Templates**
- ✅ `templates/totp/totp_setup.html` - TOTP setup page dengan QR code
- ✅ `templates/totp/totp_verify.html` - TOTP verification page
- ✅ `templates/totp/totp_backup_codes.html` - Backup codes display page

### 7. **URL Configuration** (`absensi_project/urls.py`)
- ✅ `/admin/password_change/` - custom password change view
- ✅ `/totp/check-setup/` - check TOTP setup status
- ✅ `/totp/setup/` - TOTP setup page
- ✅ `/totp/qr-code/` - QR code image
- ✅ `/totp/verify-setup/` - verify setup
- ✅ `/totp/backup-codes/` - backup codes page
- ✅ `/totp/backup-codes-acknowledge/` - acknowledge backup codes
- ✅ `/totp/verify/` - TOTP verification

### 8. **Settings Configuration** (`absensi_project/settings.py`)
- ✅ TOTP apps installed: django_otp, django_otp.plugins.otp_totp, django_otp.plugins.otp_static, two_factor
- ✅ OTPMiddleware added to MIDDLEWARE
- ✅ ForcePasswordChangeMiddleware added to MIDDLEWARE
- ✅ Custom password validator configured
- ✅ LOGIN_URL, LOGIN_REDIRECT_URL, LOGOUT_REDIRECT_URL configured

## 🔄 Authentication Flow Scenarios

### Scenario 1: First Login (Pertama Kali)
1. **Superadmin creates HRD/Direktur account** di Django Admin
   - Default password: `password123`
   - `force_password_change=True`
   - `totp_enabled=False`

2. **User login** dengan username + default password
   - ✅ Password authentication success

3. **Force Password Change**
   - 🔄 Middleware redirect ke `/admin/password_change/`
   - User harus ganti password (min 9 chars, uppercase, lowercase, symbol, no space)
   - Setelah berhasil, `force_password_change=False`

4. **TOTP Setup**
   - 🔄 Middleware redirect ke `/totp/setup/`
   - User scan QR code dengan authenticator app
   - User verify dengan 6-digit code
   - Generate 10 backup codes
   - User simpan backup codes
   - `totp_enabled=True`

5. **Login Success** → Redirect ke `/admin/`

### Scenario 2: Forgot TOTP
1. **User kehilangan akses ke authenticator app**
   - User contact superadmin

2. **Superadmin reset TOTP**
   - Select user di admin panel
   - Action: "Reset TOTP (Forgot TOTP)"
   - System delete TOTP devices
   - `totp_enabled=False`

3. **User login** dengan username + current password
   - ✅ Password authentication success
   - `force_password_change=False` (tidak perlu ganti password)

4. **TOTP Setup Ulang**
   - 🔄 Middleware redirect ke `/totp/setup/`
   - User scan QR code baru
   - User verify dengan 6-digit code
   - Generate backup codes baru
   - `totp_enabled=True`

5. **Login Success** → Redirect ke `/admin/`

### Scenario 3: Forgot Password
1. **User lupa password**
   - User contact superadmin

2. **Superadmin reset password**
   - Select user di admin panel
   - Action: "Reset Password to Default (password123)"
   - System set password ke 'password123'
   - `force_password_change=True`
   - `totp_enabled=True` (TOTP masih aktif)

3. **User login** dengan username + default password (password123)
   - ✅ Password authentication success

4. **Force Password Change**
   - 🔄 Middleware redirect ke `/admin/password_change/`
   - User harus ganti password
   - Setelah berhasil, `force_password_change=False`

5. **TOTP Verification**
   - 🔄 Middleware redirect ke `/totp/verify/`
   - User masukkan 6-digit code dari authenticator app (yang masih aktif)
   - **TIDAK perlu scan QR code lagi** karena TOTP device masih ada

6. **Login Success** → Redirect ke `/admin/`

### Scenario 4: Reset Both (Password + TOTP)
1. **User lupa both password dan TOTP**
   - User contact superadmin

2. **Superadmin reset both**
   - Select user di admin panel
   - Action: "Reset Both (Password + TOTP)"
   - System reset password ke 'password123'
   - System delete TOTP devices
   - `force_password_change=True`
   - `totp_enabled=False`

3. **Full setup flow** (sama seperti first login)
   - Login → Force password change → TOTP setup → Login success

## 🧪 Testing Guide

### Prerequisites
1. Buat user dengan role HRD atau Direktur di Django Admin
2. Install authenticator app di smartphone:
   - Google Authenticator
   - Microsoft Authenticator
   - Authy

### Test Scenario 1: First Login
```bash
1. Login ke /admin/ dengan username + password123
2. Akan redirect ke /admin/password_change/
3. Ganti password (min 9 chars, uppercase, lowercase, symbol)
4. Akan redirect ke /totp/setup/
5. Scan QR code dengan authenticator app
6. Masukkan 6-digit code untuk verify
7. Simpan 10 backup codes yang ditampilkan
8. Klik "Saya Sudah Menyimpan Backup Codes"
9. Success! Redirect ke /admin/
```

### Test Scenario 2: Normal Login (After Setup)
```bash
1. Login ke /admin/ dengan username + password
2. Akan redirect ke /totp/verify/
3. Masukkan 6-digit code dari authenticator app
4. Success! Redirect ke /admin/
```

### Test Scenario 3: Forgot TOTP
```bash
1. Superadmin: Select user → Action "Reset TOTP (Forgot TOTP)"
2. User login dengan username + password
3. Akan redirect ke /totp/setup/ (scan QR code baru)
4. Setup TOTP ulang
5. Success! Redirect ke /admin/
```

### Test Scenario 4: Forgot Password
```bash
1. Superadmin: Select user → Action "Reset Password to Default"
2. User login dengan username + password123
3. Akan redirect ke /admin/password_change/
4. Ganti password
5. Akan redirect ke /totp/verify/
6. Masukkan 6-digit code dari authenticator app LAMA (masih aktif)
7. Success! Redirect ke /admin/
```

### Test Scenario 5: Reset Both
```bash
1. Superadmin: Select user → Action "Reset Both (Password + TOTP)"
2. Full first login flow:
   - Login → Password change → TOTP setup → Success
```

### Test Scenario 6: Backup Code
```bash
1. Login normal sampai /totp/verify/
2. Masukkan backup code (bukan 6-digit code)
3. Success! Redirect ke /admin/
4. Backup code tersebut tidak bisa dipakai lagi
```

## 🔒 Security Features

1. **Password Requirements**
   - Minimum 9 karakter
   - Kompleksitas tinggi (uppercase, lowercase, symbol)
   - No spaces

2. **Password Hashing**
   - Django PBKDF2_SHA256 (870,000 iterations)
   - Automatic salting
   - Never stored in plain text

3. **TOTP Security**
   - Time-based 6-digit codes (30 seconds validity)
   - QR code only shown once
   - Device confirmation required
   - Backup codes (10x, single-use)

4. **Session Security**
   - TOTP verification required per session
   - Auto-logout after session expires
   - Session cookie protected (HttpOnly, SameSite)

5. **CSP Headers**
   - XSS protection
   - Clickjacking prevention
   - Content type sniffing prevention

## 📱 Supported Authenticator Apps

- ✅ Google Authenticator (Android/iOS)
- ✅ Microsoft Authenticator (Android/iOS)
- ✅ Authy (Android/iOS)
- ✅ Any TOTP-compatible app

## 🚀 Deployment Checklist

- [x] Database migrations applied
- [x] All dependencies installed (django-otp, qrcode, pillow, django-two-factor-auth)
- [x] Settings configured
- [x] Middleware configured
- [x] URLs configured
- [x] Templates created
- [x] Admin actions added
- [ ] Test all scenarios
- [ ] Push to GitHub

## 📝 Notes

1. **Superadmin** tidak memerlukan TOTP (only HRD dan Direktur)
2. **Backup codes** hanya ditampilkan 1x setelah setup
3. **QR code** baru di-generate setiap kali reset TOTP
4. **Password default** selalu 'password123' untuk consistency
5. **Force password change** otomatis di-set saat reset password

## 🆘 Troubleshooting

### User tidak bisa login setelah setup TOTP
- Pastikan waktu di server dan smartphone synchronized
- TOTP sangat sensitive terhadap time difference
- Coba gunakan backup code

### QR Code tidak muncul
- Check apakah django-otp dan qrcode library sudah installed
- Check browser console untuk error
- Pastikan view `totp_qr_code` accessible

### Backup codes tidak ditampilkan
- Backup codes stored di session, hanya 1x
- Jika sudah di-acknowledge, tidak bisa ditampilkan lagi
- Solusi: Reset TOTP dan setup ulang

## 📞 Support

Jika ada masalah atau pertanyaan:
1. Check Django logs untuk error messages
2. Verify database migrations applied
3. Check middleware order di settings.py
4. Test dengan user yang berbeda

---

**Status**: ✅ Implementation Complete
**Version**: 1.0
**Date**: November 6, 2025
