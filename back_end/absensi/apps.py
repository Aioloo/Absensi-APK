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
