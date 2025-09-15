from django.urls import path, include
from rest_framework.authtoken import views as auth_views 
from rest_framework.routers import DefaultRouter
from .views import KaryawanViewSet, AbsensiViewSet, TimeOffViewSet

router = DefaultRouter()
router.register(r'karyawan', KaryawanViewSet, basename='karyawan')
router.register(r'absensi', AbsensiViewSet, basename='absensi')
router.register(r'timeoff', TimeOffViewSet, basename='timeoff')

urlpatterns = [
    path('', include(router.urls))
]