"""
Script untuk update permissions admin yang sudah ada
Jalankan: python update_admin_permissions.py
"""
import os
import django

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'absensi_project.settings')
django.setup()

from absensi.models import Admin

# Update permissions untuk semua admin
admins = Admin.objects.all()
for admin in admins:
    print(f"Updating permissions for: {admin.username} ({admin.role})")
    admin._set_user_permissions()
    print(f"  ✅ Permissions updated!")

print(f"\n✅ Total {admins.count()} admin(s) updated!")
