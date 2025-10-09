#!/usr/bin/env python3
"""
Script untuk membuat test data absensi dengan berbagai tanggal
"""
import os
import sys
import django
from datetime import date, datetime, time, timedelta
import random

# Setup Django environment
sys.path.append('.')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'absensi_project.settings')
django.setup()

from django.contrib.auth.models import User
from absensi.models import Karyawan, Absensi, Perusahaan

def create_sample_attendance_data():
    """Buat sample data absensi untuk testing filter"""
    
    # Ambil semua karyawan yang ada
    karyawan_list = list(Karyawan.objects.all())
    
    if not karyawan_list:
        print("Tidak ada karyawan ditemukan. Silakan buat karyawan terlebih dahulu.")
        return
    
    # Generate data untuk 3 bulan terakhir
    end_date = date.today()
    start_date = end_date - timedelta(days=90)  # 3 bulan ke belakang
    
    current_date = start_date
    total_created = 0
    
    while current_date <= end_date:
        # Skip weekend (Sabtu=5, Minggu=6)
        if current_date.weekday() < 5:  # Monday=0 to Friday=4
            
            for karyawan in karyawan_list:
                # 70% kemungkinan karyawan hadir
                if random.random() < 0.7:
                    
                    # Random jam masuk (07:00 - 09:00)
                    jam_masuk_random = time(
                        hour=random.randint(7, 8),
                        minute=random.randint(0, 59)
                    )
                    
                    # Status berdasarkan jam masuk
                    if jam_masuk_random <= time(7, 30):
                        status_masuk = 'On Time'
                    else:
                        status_masuk = 'Telat'
                    
                    # Random jam keluar (16:00 - 18:00)
                    jam_keluar_random = time(
                        hour=random.randint(16, 17),
                        minute=random.randint(0, 59)
                    )
                    
                    status_keluar = 'On Time'  # Assume keluar selalu on time
                    
                    # Cek apakah sudah ada data absensi untuk tanggal ini
                    existing = Absensi.objects.filter(
                        karyawan=karyawan,
                        tanggal=current_date
                    ).exists()
                    
                    if not existing:
                        absensi = Absensi.objects.create(
                            karyawan=karyawan,
                            tanggal=current_date,
                            jam_masuk=jam_masuk_random,
                            jam_keluar=jam_keluar_random,
                            status_masuk=status_masuk,
                            status_keluar=status_keluar
                        )
                        total_created += 1
        
        current_date += timedelta(days=1)
    
    print(f"✅ Berhasil membuat {total_created} record absensi")
    print(f"📅 Dari tanggal: {start_date} hingga {end_date}")
    print(f"👥 Untuk {len(karyawan_list)} karyawan")
    
    # Tampilkan statistik
    total_absensi = Absensi.objects.count()
    on_time_count = Absensi.objects.filter(status_masuk='On Time').count()
    late_count = Absensi.objects.filter(status_masuk='Telat').count()
    
    print(f"\n📊 Statistik Total:")
    print(f"   Total Absensi: {total_absensi}")
    print(f"   Tepat Waktu: {on_time_count} ({on_time_count/total_absensi*100:.1f}%)")
    print(f"   Terlambat: {late_count} ({late_count/total_absensi*100:.1f}%)")

if __name__ == "__main__":
    create_sample_attendance_data()