Ini Adalah # Aplikasi Absensi Karyawan 📱

Aplikasi absensi karyawan yang terdiri dari:
- **Backend**: Django REST Framework 
- **Frontend**: Android (Kotlin + Jetpack Compose)
- **Database**: PostgreSQL

## 🚀 Setup untuk Kolaborasi

### Prerequisites
- Python 3.13+
- PostgreSQL Database
- Android Studio
- Git

### Backend Setup

1. **Clone repository**
   ```bash
   git clone https://github.com/Aioloo/Absensi-APK.git
   cd Absensi-APK
   ```

2. **Setup Python virtual environment**
   ```bash
   python -m venv venv
   # Windows
   venv\Scripts\activate
   # macOS/Linux
   source venv/bin/activate
   ```

3. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Database Configuration**
   - Buat database PostgreSQL dengan nama: `absensi_db`
   - Update kredensial database di `back_end/absensi_project/settings.py`

5. **Run migrations**
   ```bash
   cd back_end
   python manage.py migrate
   ```

6. **Create superuser** (optional)
   ```bash
   python manage.py createsuperuser
   ```

7. **Run development server**
   ```bash
   python manage.py runserver 0.0.0.0:8000
   ```

### Frontend Setup (Android)

1. **Buka Android Studio**
2. **Open project** dari folder `front_end/`
3. **Sync project** dan tunggu gradle build selesai
4. **Update Base URL** di `app/src/main/java/com/example/absensiapk/api/RetrofitClient.kt`
   ```kotlin
   const val BASE_URL = "http://YOUR_IP_ADDRESS:8000/"
   ```
5. **Run on device/emulator**

## 🔧 Konfigurasi Penting

### Network Configuration
- Server Django harus running di IP yang dapat diakses dari device Android
- Update `ALLOWED_HOSTS` di settings.py dengan IP server
- Update `BASE_URL` di RetrofitClient.kt dengan IP server

### Database Models
- **Perusahaan**: Data perusahaan
- **Karyawan**: Data karyawan terkait dengan User Django
- **Absensi**: Data check-in/check-out karyawan
- **TimeOff**: Data pengajuan cuti/izin

## 📚 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/login/` | User authentication |
| GET | `/api/karyawan/` | List semua karyawan (same company) |
| GET | `/api/karyawan/{id}/` | Detail karyawan |
| GET | `/api/absensi/history/` | History absensi user |
| POST | `/api/absensi/checkin/` | Check-in absensi |
| PATCH | `/api/absensi/{id}/checkout/` | Check-out absensi |
| GET | `/api/timeoff/history/` | History time-off user |
| POST | `/api/timeoff/ajukan/` | Ajukan cuti/izin |

## 🤝 Kolaborasi Guidelines

### Git Workflow
1. **Pull latest changes**
   ```bash
   git pull origin master
   ```

2. **Create feature branch**
   ```bash
   git checkout -b feature/nama-fitur
   ```

3. **Commit changes**
   ```bash
   git add .
   git commit -m "feat: add new feature"
   ```

4. **Push to repository**
   ```bash
   git push origin feature/nama-fitur
   ```

### File yang Tidak Di-track (sudah ada di .gitignore)
- `__pycache__/` - Python cache files
- `.idea/` - IDE configuration files  
- `build/` - Android build files
- `local.properties` - Android local config
- `media/` - User uploaded files
- `*.log` - Log files
- Test/debug files

### Commit Message Format
- `feat:` - New features
- `fix:` - Bug fixes
- `docs:` - Documentation
- `style:` - Code formatting
- `refactor:` - Code refactoring

## 📱 Features

### Backend Features
- JWT Authentication
- CORS enabled untuk Android app
- File upload untuk foto profil & absensi
- Time-off management dengan approval system
- Company-based employee filtering

### Android Features  
- Modern UI dengan Jetpack Compose
- Camera integration untuk foto absensi
- GPS location untuk check-in/out
- Offline-first dengan caching
- Material Design 3

## 🐛 Troubleshooting

### Common Issues
1. **Network Error saat login dari Android**
   - Pastikan server Django running
   - Check BASE_URL di RetrofitClient.kt
   - Pastikan ALLOWED_HOSTS di settings.py

2. **Authentication Error**
   - Check JWT token expiration
   - Verify user credentials
   
3. **Migration Error**
   - Run `python manage.py migrate`
   - Check database connection

## 📞 Support
- Buat issue di GitHub untuk bug reports
- Untuk pertanyaan, gunakan Discussions

---
**Happy Coding! 🎉** yang didapat dari magang di PT PAL Indonesia
