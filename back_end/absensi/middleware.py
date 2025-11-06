"""
Security Middleware untuk Absensi Project
- Content Security Policy (CSP) headers untuk keamanan
- Force Password Change untuk TOTP authentication
"""

from django.shortcuts import redirect
from django.urls import reverse


class ContentSecurityPolicyMiddleware:
    """
    Middleware untuk menambahkan Content Security Policy (CSP) header
    CSP membantu melindungi aplikasi dari XSS (Cross-Site Scripting) attacks
    """
    
    def __init__(self, get_response):
        self.get_response = get_response
    
    def __call__(self, request):
        response = self.get_response(request)
        
        # Path-path yang memerlukan CSP protection
        protected_paths = [
            '/admin',
            '/admin/',
            '/admin/login/',
            '/robots.txt',
            '/sitemap.xml',
        ]
        
        # Check jika request path adalah salah satu dari protected paths
        # Atau jika path dimulai dengan /admin (untuk semua subpath admin)
        should_add_csp = False
        for path in protected_paths:
            if request.path == path or request.path.startswith('/admin'):
                should_add_csp = True
                break
        
        if should_add_csp:
            # CSP Policy yang ketat untuk keamanan
            csp_policy = (
                "default-src 'self'; "  # Default: hanya allow dari same origin
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "  # Scripts: allow inline untuk Django admin
                "style-src 'self' 'unsafe-inline'; "  # Styles: allow inline CSS
                "img-src 'self' data: https:; "  # Images: allow dari self, data URLs, dan HTTPS
                "font-src 'self' data:; "  # Fonts: allow dari self dan data URLs
                "connect-src 'self'; "  # AJAX/WebSocket: hanya ke same origin
                "frame-ancestors 'self'; "  # Prevent clickjacking
                "form-action 'self'; "  # Forms hanya submit ke same origin
                "base-uri 'self'; "  # Prevent base tag injection
                "object-src 'none'; "  # Block plugins (Flash, Java, etc)
            )
            
            response['Content-Security-Policy'] = csp_policy
            
            # Tambahan security headers untuk defense in depth
            response['X-Content-Type-Options'] = 'nosniff'  # Prevent MIME sniffing
            response['X-Frame-Options'] = 'SAMEORIGIN'  # Prevent clickjacking
            response['X-XSS-Protection'] = '1; mode=block'  # Enable XSS filter
            response['Referrer-Policy'] = 'strict-origin-when-cross-origin'  # Control referrer info
        
        return response


class ForcePasswordChangeMiddleware:
    """
    Middleware untuk memaksa user mengganti password setelah:
    1. First login (default password)
    2. Forgot password (reset by superadmin)
    3. Reset both (password + TOTP reset by superadmin)
    """
    
    def __init__(self, get_response):
        self.get_response = get_response
    
    def __call__(self, request):
        # Path yang dikecualikan dari redirect
        excluded_paths = [
            reverse('admin:password_change'),
            reverse('admin:password_change_done'),
            reverse('admin:logout'),
            '/admin/jsi18n/',  # Django i18n JavaScript catalog
            '/totp/setup/',
            '/totp/verify/',
            '/totp/qr-code/',
            '/totp/verify-setup/',
            '/totp/backup-codes/',
            '/totp/backup-codes-acknowledge/',
            '/totp/check-setup/',
        ]
        
        # Jangan redirect jika user belum login atau di excluded paths
        if not request.user.is_authenticated:
            return self.get_response(request)
        
        if request.path in excluded_paths:
            return self.get_response(request)
        
        # PENTING: Skip middleware untuk superadmin
        # Superadmin tidak perlu TOTP dan bisa akses admin panel bebas
        if request.user.is_superuser:
            return self.get_response(request)
        
        # Check apakah user adalah Admin (HRD/Direktur) dengan force_password_change=True
        try:
            if hasattr(request.user, 'admin_account'):
                admin = request.user.admin_account
                
                # Priority 1: Force password change
                if admin.force_password_change:
                    # Redirect ke password change page
                    return redirect('admin:password_change')
                
                # Priority 2: TOTP verification (jika sudah setup tapi belum verified di session)
                # Jika TOTP sudah enabled tapi belum verified di session ini
                if admin.totp_enabled and not request.user.is_verified():
                    # Redirect ke TOTP verification
                    return redirect('totp_verify')
                
                # Priority 3: TOTP setup (jika belum di-setup sama sekali)
                # Jika TOTP belum di-setup (totp_enabled=False)
                if not admin.totp_enabled:
                    # Redirect ke TOTP setup
                    return redirect('totp_check_setup')
        except Exception as e:
            # Jika terjadi error, lanjutkan normal (untuk keamanan)
            # Log error jika perlu
            pass
        
        return self.get_response(request)
