from django.contrib import admin
from django.urls import path, include
from django.shortcuts import redirect
from absensi.views import LoginView, home_view
from absensi.totp_views import (
    CustomPasswordChangeView,
    totp_check_setup,
    totp_setup,
    totp_qr_code,
    totp_verify_setup,
    totp_backup_codes,
    totp_backup_codes_acknowledge,
    totp_verify,
)
from absensi_project import settings
from django.conf.urls.static import static

urlpatterns = [
    path('admin/password_change/', CustomPasswordChangeView.as_view(), name='admin_password_change'),
    path('admin/', admin.site.urls),
    path('api/login/', LoginView.as_view(), name='login'),
    path('api/', include('absensi.urls')),
    path('', lambda request: redirect('/admin/'), name='home'),
    
    # TOTP URLs
    path('totp/check-setup/', totp_check_setup, name='totp_check_setup'),
    path('totp/setup/', totp_setup, name='totp_setup'),
    path('totp/qr-code/', totp_qr_code, name='totp_qr_code'),
    path('totp/verify-setup/', totp_verify_setup, name='totp_verify_setup'),
    path('totp/backup-codes/', totp_backup_codes, name='totp_backup_codes'),
    path('totp/backup-codes-acknowledge/', totp_backup_codes_acknowledge, name='totp_backup_codes_acknowledge'),
    path('totp/verify/', totp_verify, name='totp_verify'),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)    