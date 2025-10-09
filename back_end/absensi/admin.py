from django.contrib import admin
from django.contrib.auth.models import Group
from django.utils import timezone
from django.utils.safestring import mark_safe
from datetime import datetime, timedelta
from django.db import models
from django.http import HttpResponse
from django.urls import path
from django.shortcuts import redirect
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from .models import Karyawan, Absensi, TimeOff, Perusahaan, SecurityAuditLog

# Unregister Groups model karena tidak digunakan
admin.site.unregister(Group)

# Custom Filter untuk Bulan
class MonthFilter(admin.SimpleListFilter):
    title = 'Bulan'
    parameter_name = 'bulan'

    def lookups(self, request, model_admin):
        """Return pilihan bulan untuk filter berdasarkan data yang ada"""
        months = []
        
        # Ambil range tahun dari data yang ada di database
        years_in_db = model_admin.model.objects.dates('tanggal', 'year')
        
        if years_in_db:
            # Jika ada data, gunakan tahun dari data + tahun sekarang
            min_year = min(years_in_db).year
            max_year = max(years_in_db).year
            current_year = timezone.now().year
            
            # Pastikan tahun sekarang dan tahun depan juga tersedia
            all_years = sorted(set([min_year, max_year, current_year, current_year + 1]))
            
            # Generate semua bulan untuk tahun-tahun tersebut
            for year in all_years:
                for month in range(1, 13):
                    month_name = datetime(year, month, 1).strftime('%B %Y')
                    month_value = f"{year}-{month:02d}"
                    months.append((month_value, month_name))
        else:
            # Jika belum ada data, tampilkan tahun sekarang saja
            current_year = timezone.now().year
            for month in range(1, 13):
                month_name = datetime(current_year, month, 1).strftime('%B %Y')
                month_value = f"{current_year}-{month:02d}"
                months.append((month_value, month_name))
        
        return months

    def queryset(self, request, queryset):
        """Filter queryset berdasarkan bulan yang dipilih"""
        if self.value():
            try:
                year, month = self.value().split('-')
                year, month = int(year), int(month)
                return queryset.filter(
                    tanggal__year=year,
                    tanggal__month=month
                )
            except (ValueError, AttributeError):
                pass
        return queryset

# Custom Filter untuk Minggu
class WeekFilter(admin.SimpleListFilter):
    title = 'Minggu'
    parameter_name = 'minggu'

    def lookups(self, request, model_admin):
        """Return pilihan minggu untuk filter berdasarkan data yang ada"""
        weeks = []
        today = timezone.now().date()
        
        # Ambil range tanggal dari data yang ada di database
        dates_in_db = model_admin.model.objects.dates('tanggal', 'day')
        
        if dates_in_db:
            # Ambil 20 minggu terakhir berdasarkan data + beberapa minggu ke depan
            min_date = min(dates_in_db)
            max_date = max(dates_in_db)
            
            # Mulai dari minggu terakhir yang ada data, mundur 10 minggu, maju 4 minggu
            latest_week_start = max_date - timedelta(days=max_date.weekday())
            earliest_display = latest_week_start - timedelta(weeks=10)
            latest_display = latest_week_start + timedelta(weeks=4)
            
            current_week = earliest_display
            while current_week <= latest_display:
                week_end = current_week + timedelta(days=6)
                week_label = f"{current_week.strftime('%d %b')} - {week_end.strftime('%d %b %Y')}"
                week_value = current_week.strftime('%Y-W%U')  # Format lebih standard
                weeks.append((week_value, week_label))
                current_week += timedelta(weeks=1)
        else:
            # Jika belum ada data, tampilkan minggu sekarang saja
            week_start = today - timedelta(days=today.weekday())
            week_end = week_start + timedelta(days=6)
            week_label = f"{week_start.strftime('%d %b')} - {week_end.strftime('%d %b %Y')}"
            week_value = week_start.strftime('%Y-W%U')
            weeks.append((week_value, week_label))
        
        return weeks

    def queryset(self, request, queryset):
        """Filter queryset berdasarkan minggu yang dipilih"""
        if self.value():
            try:
                # Format: 2025-W40
                year_week = self.value().replace('W', '')
                year, week = year_week.split('-')
                year, week = int(year), int(week)
                
                # Hitung tanggal awal minggu (Minggu sebagai hari pertama)
                jan_1 = datetime(year, 1, 1).date()
                days_to_sunday = (6 - jan_1.weekday()) % 7  # Hari ke minggu pertama
                first_sunday = jan_1 + timedelta(days=days_to_sunday)
                week_start = first_sunday + timedelta(weeks=week)
                week_end = week_start + timedelta(days=6)
                
                return queryset.filter(
                    tanggal__gte=week_start,
                    tanggal__lte=week_end
                )
            except (ValueError, AttributeError):
                pass
        return queryset

# Custom Filter untuk Quarter (Bonus)
class QuarterFilter(admin.SimpleListFilter):
    title = 'Kuartal'
    parameter_name = 'kuartal'

    def lookups(self, request, model_admin):
        """Return pilihan kuartal untuk filter berdasarkan data yang ada"""
        quarters = []
        
        # Ambil range tahun dari data yang ada di database
        years_in_db = model_admin.model.objects.dates('tanggal', 'year')
        
        if years_in_db:
            # Jika ada data, gunakan tahun dari data + tahun sekarang
            min_year = min(years_in_db).year
            max_year = max(years_in_db).year
            current_year = timezone.now().year
            
            # Pastikan tahun sekarang dan tahun depan juga tersedia
            all_years = sorted(set([min_year, max_year, current_year, current_year + 1]))
            
            # Generate semua kuartal untuk tahun-tahun tersebut
            for year in all_years:
                quarters.extend([
                    (f"{year}-Q1", f"Q1 {year} (Jan-Mar)"),
                    (f"{year}-Q2", f"Q2 {year} (Apr-Jun)"),
                    (f"{year}-Q3", f"Q3 {year} (Jul-Sep)"),
                    (f"{year}-Q4", f"Q4 {year} (Oct-Des)"),
                ])
        else:
            # Jika belum ada data, tampilkan tahun sekarang saja
            current_year = timezone.now().year
            quarters.extend([
                (f"{current_year}-Q1", f"Q1 {current_year} (Jan-Mar)"),
                (f"{current_year}-Q2", f"Q2 {current_year} (Apr-Jun)"),
                (f"{current_year}-Q3", f"Q3 {current_year} (Jul-Sep)"),
                (f"{current_year}-Q4", f"Q4 {current_year} (Oct-Des)"),
            ])
        
        return quarters

    def queryset(self, request, queryset):
        """Filter queryset berdasarkan kuartal yang dipilih"""
        if self.value():
            try:
                year, quarter = self.value().split('-Q')
                year, quarter = int(year), int(quarter)
                
                quarter_months = {
                    1: [1, 2, 3],
                    2: [4, 5, 6], 
                    3: [7, 8, 9],
                    4: [10, 11, 12]
                }
                
                months = quarter_months.get(quarter, [])
                if months:
                    return queryset.filter(
                        tanggal__year=year,
                        tanggal__month__in=months
                    )
            except (ValueError, AttributeError):
                pass
        return queryset

# Kustomisasi model Perusahaan
@admin.register(Perusahaan)
class PerusahaanAdmin(admin.ModelAdmin):
    list_display = ('nama', 'kode', 'created_at')
    search_fields = ('nama', 'kode')
    ordering = ['nama']

# Kustomisasi model Karyawan
@admin.register(Karyawan)
class KaryawanAdmin(admin.ModelAdmin):
    # Field yang akan ditampilkan di halaman daftar
    list_display = ('nama', 'perusahaan', 'divisi', 'email')
    # Field yang bisa dicari
    search_fields = ('nama', 'perusahaan__nama', 'divisi', 'email')
    # Filter berdasarkan perusahaan
    list_filter = ('perusahaan', 'divisi')
    
    def get_queryset(self, request):
        """Filter data berdasarkan perusahaan user yang login"""
        qs = super().get_queryset(request)
        if request.user.is_superuser:
            return qs
        # Jika user bukan superuser, hanya tampilkan karyawan dari perusahaan yang sama
        try:
            karyawan = Karyawan.objects.get(user=request.user)
            return qs.filter(perusahaan=karyawan.perusahaan)
        except Karyawan.DoesNotExist:
            return qs.none()
    
    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        """Batasi pilihan perusahaan berdasarkan user yang login"""
        if db_field.name == "perusahaan":
            if not request.user.is_superuser:
                try:
                    karyawan = Karyawan.objects.get(user=request.user)
                    kwargs["queryset"] = Perusahaan.objects.filter(id=karyawan.perusahaan.id)
                except Karyawan.DoesNotExist:
                    kwargs["queryset"] = Perusahaan.objects.none()
        return super().formfield_for_foreignkey(db_field, request, **kwargs)

# Kustomisasi model Absensi
@admin.register(Absensi)
class AbsensiAdmin(admin.ModelAdmin):
    list_display = (
        'karyawan', 'tanggal', 'jam_masuk', 'jam_keluar', 
        'status_masuk', 'status_keluar', 'get_durasi_kerja'
    )
    list_filter = (
        MonthFilter,           # Filter Bulan
        WeekFilter,            # Filter Minggu  
        QuarterFilter,         # Filter Kuartal
        'tanggal',             # Filter Tanggal (default Django)
        'status_masuk', 
        'status_keluar',
        'karyawan__perusahaan',
        'karyawan__divisi'
    )
    search_fields = ('karyawan__nama', 'karyawan__email')
    date_hierarchy = 'tanggal'  # Navigation berdasarkan tanggal
    ordering = ['-tanggal', '-jam_masuk']
    list_per_page = 50  # Pagination
    
    # Actions untuk bulk operations
    actions = ['export_to_csv', 'export_to_excel', 'mark_as_present']
    
    def get_urls(self):
        """Add custom URLs for export functions"""
        urls = super().get_urls()
        custom_urls = [
            path('export-excel/', self.admin_site.admin_view(self.export_excel_view), name='absensi_export_excel'),
            path('export-csv/', self.admin_site.admin_view(self.export_csv_view), name='absensi_export_csv'),
        ]
        return custom_urls + urls
    
    def export_excel_view(self, request):
        """Custom view for Excel export with current filters"""
        # Get the changelist instance with current filters
        changelist_instance = self.get_changelist_instance(request)
        queryset = changelist_instance.get_queryset(request)
        
        return self.create_excel_response(request, queryset)
    
    def export_csv_view(self, request):
        """Custom view for CSV export with current filters"""
        # Get the changelist instance with current filters
        changelist_instance = self.get_changelist_instance(request)
        queryset = changelist_instance.get_queryset(request)
        
        return self.create_csv_response(request, queryset)
    
    # Fieldsets untuk form edit/add
    fieldsets = (
        ('Informasi Karyawan', {
            'fields': ('karyawan',)
        }),
        ('Waktu Absensi', {
            'fields': (('jam_masuk', 'jam_keluar'),)
        }),
        ('Status', {
            'fields': (('status_masuk', 'status_keluar'),)
        }),
        ('Lokasi GPS', {
            'fields': (
                ('lokasi_masuk_lat', 'lokasi_masuk_long'),
                ('lokasi_keluar_lat', 'lokasi_keluar_long')
            ),
            'classes': ('collapse',)  # Collapsed by default
        }),
        ('Foto Absensi', {
            'fields': (('foto_masuk', 'foto_keluar'),),
            'classes': ('collapse',)  # Collapsed by default
        }),
    )
    
    def get_queryset(self, request):
        """Filter data berdasarkan perusahaan user yang login"""
        qs = super().get_queryset(request)
        if request.user.is_superuser:
            return qs
        try:
            karyawan = Karyawan.objects.get(user=request.user)
            return qs.filter(karyawan__perusahaan=karyawan.perusahaan)
        except Karyawan.DoesNotExist:
            return qs.none()
    
    def get_durasi_kerja(self, obj):
        """Hitung durasi kerja berdasarkan jam masuk dan keluar"""
        if obj.jam_masuk and obj.jam_keluar:
            # Convert time to datetime untuk calculation
            jam_masuk_dt = datetime.combine(obj.tanggal, obj.jam_masuk)
            jam_keluar_dt = datetime.combine(obj.tanggal, obj.jam_keluar)
            
            # Handle case jika jam keluar di hari berikutnya
            if jam_keluar_dt < jam_masuk_dt:
                jam_keluar_dt += timedelta(days=1)
            
            durasi = jam_keluar_dt - jam_masuk_dt
            hours, remainder = divmod(durasi.total_seconds(), 3600)
            minutes = remainder // 60
            return f"{int(hours)}j {int(minutes)}m"
        return "-"
    
    get_durasi_kerja.short_description = "Durasi Kerja"
    get_durasi_kerja.admin_order_field = 'jam_keluar'
    
    @admin.action(description="Export data absensi ke CSV")
    def export_to_csv(self, request, queryset):
        """Export filtered absensi data to CSV"""
        # Get the changelist instance to access the filtered queryset
        changelist_instance = self.get_changelist_instance(request)
        filtered_queryset = changelist_instance.get_queryset(request)
        
        return self.create_csv_response(request, filtered_queryset)
    
    def create_csv_response(self, request, queryset):
        """Create CSV response with formatted data"""
        import csv
        from django.http import HttpResponse
        
        response = HttpResponse(content_type='text/csv')
        
        # Generate filename based on current filters
        filename = self.get_export_filename(request, "csv")
        response['Content-Disposition'] = f'attachment; filename="{filename}"'
        
        writer = csv.writer(response)
        writer.writerow([
            'No', 'Nama Karyawan', 'Perusahaan', 'Divisi', 'Tanggal', 
            'Jam Masuk', 'Status Masuk', 'Jam Keluar', 'Status Keluar', 
            'Durasi Kerja', 'Lokasi Masuk', 'Lokasi Keluar'
        ])
        
        for index, obj in enumerate(queryset.order_by('tanggal', 'karyawan__nama'), 1):
            writer.writerow([
                index,
                obj.karyawan.nama,
                obj.karyawan.perusahaan.nama,
                obj.karyawan.divisi or '-',
                obj.tanggal.strftime('%d/%m/%Y'),
                obj.jam_masuk.strftime('%H:%M') if obj.jam_masuk else '-',
                obj.status_masuk or '-',
                obj.jam_keluar.strftime('%H:%M') if obj.jam_keluar else '-',
                obj.status_keluar or '-',
                self.get_durasi_kerja(obj),
                f"{obj.lokasi_masuk_lat}, {obj.lokasi_masuk_long}" if obj.lokasi_masuk_lat and obj.lokasi_masuk_long else '-',
                f"{obj.lokasi_keluar_lat}, {obj.lokasi_keluar_long}" if obj.lokasi_keluar_lat and obj.lokasi_keluar_long else '-'
            ])
        
        return response
    
    @admin.action(description="Export ke Excel")
    def export_to_excel(self, request, queryset):
        """Export filtered absensi data to Excel"""
        # Get the changelist instance to access the filtered queryset
        changelist_instance = self.get_changelist_instance(request)
        filtered_queryset = changelist_instance.get_queryset(request)
        
        return self.create_excel_response(request, filtered_queryset)
    
    def create_excel_response(self, request, queryset):
        """Create Excel response with formatted data"""
        # Create workbook and active worksheet
        wb = Workbook()
        ws = wb.active
        ws.title = "Data Absensi"
        
        # Define styles
        header_font = Font(bold=True, color='FFFFFF')
        header_fill = PatternFill(start_color='366092', end_color='366092', fill_type='solid')
        border = Border(
            left=Side(style='thin'),
            right=Side(style='thin'),
            top=Side(style='thin'),
            bottom=Side(style='thin')
        )
        center_alignment = Alignment(horizontal='center', vertical='center')
        
        # Headers
        headers = [
            'No', 'Nama Karyawan', 'Perusahaan', 'Divisi', 'Tanggal', 
            'Jam Masuk', 'Status Masuk', 'Jam Keluar', 'Status Keluar', 
            'Durasi Kerja', 'Lokasi Masuk', 'Lokasi Keluar'
        ]
        
        # Add headers to worksheet
        for col_num, header in enumerate(headers, 1):
            cell = ws.cell(row=1, column=col_num)
            cell.value = header
            cell.font = header_font
            cell.fill = header_fill
            cell.border = border
            cell.alignment = center_alignment
        
        # Add data rows
        row_num = 2
        for index, obj in enumerate(queryset.order_by('tanggal', 'karyawan__nama'), 1):
            data = [
                index,
                obj.karyawan.nama,
                obj.karyawan.perusahaan.nama,
                obj.karyawan.divisi or '-',
                obj.tanggal.strftime('%d/%m/%Y'),
                obj.jam_masuk.strftime('%H:%M') if obj.jam_masuk else '-',
                obj.status_masuk or '-',
                obj.jam_keluar.strftime('%H:%M') if obj.jam_keluar else '-',
                obj.status_keluar or '-',
                self.get_durasi_kerja(obj),
                f"{obj.lokasi_masuk_lat}, {obj.lokasi_masuk_long}" if obj.lokasi_masuk_lat and obj.lokasi_masuk_long else '-',
                f"{obj.lokasi_keluar_lat}, {obj.lokasi_keluar_long}" if obj.lokasi_keluar_lat and obj.lokasi_keluar_long else '-'
            ]
            
            for col_num, value in enumerate(data, 1):
                cell = ws.cell(row=row_num, column=col_num)
                cell.value = value
                cell.border = border
                if col_num == 1:  # Nomor urut
                    cell.alignment = center_alignment
                elif col_num in [5, 6, 8]:  # Tanggal dan jam
                    cell.alignment = center_alignment
            
            row_num += 1
        
        # Adjust column widths
        column_widths = [5, 20, 15, 12, 12, 10, 12, 10, 12, 12, 15, 15]
        for col_num, width in enumerate(column_widths, 1):
            ws.column_dimensions[get_column_letter(col_num)].width = width
        
        # Add summary row if there's data
        if queryset.exists():
            summary_row = row_num + 1
            ws.cell(row=summary_row, column=1, value="RINGKASAN:")
            ws.cell(row=summary_row, column=1).font = Font(bold=True)
            
            total_records = queryset.count()
            on_time_count = queryset.filter(status_masuk='On Time').count()
            late_count = queryset.filter(status_masuk='Telat').count()
            
            ws.cell(row=summary_row + 1, column=1, value=f"Total Record: {total_records}")
            ws.cell(row=summary_row + 2, column=1, value=f"Tepat Waktu: {on_time_count}")
            ws.cell(row=summary_row + 3, column=1, value=f"Terlambat: {late_count}")
        
        # Create HTTP response
        response = HttpResponse(content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
        
        # Generate filename based on current filters
        filename = self.get_export_filename(request, "xlsx")
        response['Content-Disposition'] = f'attachment; filename="{filename}"'
        
        # Save workbook to response
        wb.save(response)
        return response
    
    def get_export_filename(self, request, extension):
        """Generate dynamic filename based on active filters"""
        params = request.GET
        filename_parts = ["absensi"]
        
        # Check for month filter
        if 'bulan' in params and params['bulan']:
            bulan_value = params['bulan']  # Format: 2025-08
            try:
                year, month = bulan_value.split('-')
                month_names = {
                    '01': 'Januari', '02': 'Februari', '03': 'Maret', '04': 'April',
                    '05': 'Mei', '06': 'Juni', '07': 'Juli', '08': 'Agustus',
                    '09': 'September', '10': 'Oktober', '11': 'November', '12': 'Desember'
                }
                filename_parts.append(f"{month_names.get(month, month)}_{year}")
            except:
                filename_parts.append(bulan_value.replace('-', '_'))
        
        # Check for week filter
        elif 'minggu' in params and params['minggu']:
            minggu_value = params['minggu']  # Format: 2025-W40
            filename_parts.append(f"minggu_{minggu_value.replace('-', '_')}")
        
        # Check for quarter filter
        elif 'kuartal' in params and params['kuartal']:
            kuartal_value = params['kuartal']  # Format: 2025-Q3
            filename_parts.append(f"kuartal_{kuartal_value.replace('-', '_')}")
        
        # Check for year filter
        elif 'tanggal__year' in params and params['tanggal__year']:
            year = params['tanggal__year']
            filename_parts.append(f"tahun_{year}")
        
        # Check for specific month/year combination
        elif 'tanggal__month' in params and 'tanggal__year' in params:
            year = params['tanggal__year']
            month = params['tanggal__month']
            month_names = {
                '1': 'Januari', '2': 'Februari', '3': 'Maret', '4': 'April',
                '5': 'Mei', '6': 'Juni', '7': 'Juli', '8': 'Agustus',
                '9': 'September', '10': 'Oktober', '11': 'November', '12': 'Desember'
            }
            filename_parts.append(f"{month_names.get(month, month)}_{year}")
        
        # Check for company filter
        if 'karyawan__perusahaan__id__exact' in params:
            try:
                perusahaan_id = params['karyawan__perusahaan__id__exact']
                perusahaan = Perusahaan.objects.get(id=perusahaan_id)
                filename_parts.append(perusahaan.nama.replace(' ', '_'))
            except:
                pass
        
        # Add timestamp if no specific filter
        if len(filename_parts) == 1:
            from datetime import datetime
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename_parts.append(f"all_data_{timestamp}")
        
        return f"{'_'.join(filename_parts)}.{extension}"
    
    @admin.action(description="Tandai sebagai hadir (manual)")
    def mark_as_present(self, request, queryset):
        """Manually mark selected records as present"""
        updated = queryset.update(status_masuk='On Time')
        self.message_user(request, f"{updated} record ditandai sebagai hadir.")
    
    def changelist_view(self, request, extra_context=None):
        """Add summary statistics to changelist view"""
        extra_context = extra_context or {}
        
        # Get filtered queryset
        cl = self.get_changelist_instance(request)
        queryset = cl.get_queryset(request)
        
        # Calculate statistics
        total_records = queryset.count()
        on_time_count = queryset.filter(status_masuk='On Time').count()
        late_count = queryset.filter(status_masuk='Telat').count()
        
        # Calculate percentages
        on_time_percentage = (on_time_count / total_records * 100) if total_records > 0 else 0
        late_percentage = (late_count / total_records * 100) if total_records > 0 else 0
        
        # Get current month/week stats
        today = timezone.now().date()
        current_month_count = queryset.filter(
            tanggal__year=today.year,
            tanggal__month=today.month
        ).count()
        
        week_start = today - timedelta(days=today.weekday())
        current_week_count = queryset.filter(
            tanggal__gte=week_start,
            tanggal__lt=week_start + timedelta(days=7)
        ).count()
        
        extra_context['absensi_stats'] = {
            'total_records': total_records,
            'on_time_count': on_time_count,
            'late_count': late_count,
            'on_time_percentage': round(on_time_percentage, 1),
            'late_percentage': round(late_percentage, 1),
            'current_month_count': current_month_count,
            'current_week_count': current_week_count,
        }
        
        return super().changelist_view(request, extra_context=extra_context)

# Kustomisasi model TimeOff
@admin.register(TimeOff)
class TimeOffAdmin(admin.ModelAdmin):
    list_display = ('karyawan', 'jenis', 'tanggal_mulai', 'status')
    list_filter = ('jenis', 'status', 'karyawan__perusahaan')
    search_fields = ('karyawan__nama',)
    actions = ['approve_requests', 'reject_requests']
    
    def get_queryset(self, request):
        """Filter data berdasarkan perusahaan user yang login"""
        qs = super().get_queryset(request)
        if request.user.is_superuser:
            return qs
        try:
            karyawan = Karyawan.objects.get(user=request.user)
            return qs.filter(karyawan__perusahaan=karyawan.perusahaan)
        except Karyawan.DoesNotExist:
            return qs.none()
    
    @admin.action(description="Setujui pengajuan yang dipilih")
    def approve_requests(self, request, queryset):
        queryset.update(status='Approved')
        self.message_user(request, f"{queryset.count()} pengajuan telah disetujui.")
    
    @admin.action(description="Tolak pengajuan yang dipilih")
    def reject_requests(self, request, queryset):
        queryset.update(status='Rejected')
        self.message_user(request, f"{queryset.count()} pengajuan telah ditolak.")


# 🔐 ADMIN UNTUK SECURITY AUDIT LOG
@admin.register(SecurityAuditLog)
class SecurityAuditLogAdmin(admin.ModelAdmin):
    list_display = [
        'created_at', 'karyawan', 'audit_type', 'severity', 'status',
        'time_difference_display', 'location_display', 'description_short'
    ]
    
    list_filter = [
        'audit_type', 'severity', 'status', 'created_at',
        ('karyawan__perusahaan', admin.RelatedOnlyFieldListFilter),
    ]
    
    search_fields = [
        'karyawan__nama', 'karyawan__user__username', 
        'description', 'ip_address'
    ]
    
    readonly_fields = [
        'created_at', 'karyawan', 'audit_type', 'client_time', 
        'server_time', 'time_difference_minutes', 'client_lat', 
        'client_lng', 'user_agent', 'ip_address', 'description'
    ]
    
    fields = [
        ('karyawan', 'audit_type', 'severity'),
        ('client_time', 'server_time', 'time_difference_minutes'),
        ('client_lat', 'client_lng'),
        ('user_agent', 'ip_address'),
        'description',
        ('status', 'reviewed_by'),
        'notes',
        'created_at'
    ]
    
    date_hierarchy = 'created_at'
    
    def time_difference_display(self, obj):
        if obj.time_difference_minutes:
            if obj.time_difference_minutes > 10:
                return mark_safe(f'<span style="color: red; font-weight: bold;">{obj.time_difference_minutes} menit</span>')
            elif obj.time_difference_minutes > 5:
                return mark_safe(f'<span style="color: orange;">{obj.time_difference_minutes} menit</span>')
            else:
                return f'{obj.time_difference_minutes} menit'
        return '-'
    time_difference_display.short_description = 'Selisih Waktu'
    
    def location_display(self, obj):
        if obj.client_lat and obj.client_lng:
            return f'({obj.client_lat:.4f}, {obj.client_lng:.4f})'
        return '-'
    location_display.short_description = 'Lokasi Client'
    
    def description_short(self, obj):
        if len(obj.description) > 50:
            return obj.description[:50] + '...'
        return obj.description
    description_short.short_description = 'Deskripsi'
    
    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if request.user.is_superuser:
            return qs
        try:
            karyawan = Karyawan.objects.get(user=request.user)
            return qs.filter(karyawan__perusahaan=karyawan.perusahaan)
        except Karyawan.DoesNotExist:
            return qs.none()
    
    @admin.action(description="Tandai sebagai sudah direview")
    def mark_as_reviewed(self, request, queryset):
        queryset.update(status='REVIEWED', reviewed_by=request.user)
        self.message_user(request, f"{queryset.count()} log audit telah ditandai sebagai sudah direview.")
    
    @admin.action(description="Tandai sebagai false positive")
    def mark_as_false_positive(self, request, queryset):
        queryset.update(status='FALSE_POSITIVE', reviewed_by=request.user)
        self.message_user(request, f"{queryset.count()} log audit telah ditandai sebagai false positive.")
    
    actions = ['mark_as_reviewed', 'mark_as_false_positive']