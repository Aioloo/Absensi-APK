"""
Security Middleware untuk Absensi Project
Menambahkan Content Security Policy (CSP) headers untuk keamanan
"""

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
