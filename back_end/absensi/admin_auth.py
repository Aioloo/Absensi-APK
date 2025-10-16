"""
Custom admin configuration untuk memindahkan Admin model ke Authentication and Authorization
"""
from django.contrib import admin
from django.contrib.admin.sites import site
from .models import Admin

# Unregister Admin dari absensi app jika sudah terdaftar
try:
    admin.site.unregister(Admin)
except:
    pass

# Buat proxy model dengan app_label 'auth'
class AdminProxy(Admin):
    class Meta:
        proxy = True
        app_label = 'auth'
        verbose_name = 'Admin'
        verbose_name_plural = 'Admins'

# Import AdminModelAdmin dari admin.py
from .admin import AdminModelAdmin

# Register proxy model
admin.site.register(AdminProxy, AdminModelAdmin)
