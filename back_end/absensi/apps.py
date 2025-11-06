from django.apps import AppConfig


class AbsensiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'absensi'
    
    def ready(self):
        # Import admin_auth untuk memindahkan Admin ke Authentication section
        try:
            import absensi.admin_auth
        except ImportError:
            pass
        
        # Unregister OTP models dari admin panel untuk keamanan
        from django.contrib import admin
        
        try:
            from django_otp.plugins.otp_static.models import StaticDevice, StaticToken
            from django_otp.plugins.otp_totp.models import TOTPDevice
            
            # Unregister models
            models_to_unregister = [StaticDevice, StaticToken, TOTPDevice]
            
            for model in models_to_unregister:
                try:
                    admin.site.unregister(model)
                except admin.sites.NotRegistered:
                    pass
        except ImportError:
            pass
