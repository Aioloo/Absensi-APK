# TOTP Middleware Flow Test Guide

## Test Scenarios

### Scenario 1: Superadmin Login
**User:** Superadmin (is_superuser=True)
**Expected:** 
- ✅ Langsung bisa akses admin panel
- ✅ TIDAK ada redirect ke TOTP
- ✅ TIDAK terpengaruh middleware

### Scenario 2: HRD/Direktur Login (First Time)
**User:** HRD/Direktur dengan force_password_change=True, totp_enabled=False
**Expected:**
1. Login dengan password123
2. Redirect ke `/admin/password_change/` (Priority 1)
3. Setelah ganti password → Redirect ke `/totp/setup/` (Priority 3)
4. Setelah setup TOTP → Redirect ke `/admin/`

### Scenario 3: HRD/Direktur Login (Normal - TOTP Already Setup)
**User:** HRD/Direktur dengan force_password_change=False, totp_enabled=True
**Expected:**
1. Login dengan password
2. Redirect ke `/totp/verify/` (Priority 2)
3. Setelah verify TOTP → Redirect ke `/admin/`

### Scenario 4: Multiple Users Login Simultaneously
**Users:** 
- User A: HRD (sedang setup TOTP)
- User B: Direktur (sudah setup TOTP, mau login)
- User C: Superadmin

**Expected:**
- ✅ User A: Di halaman `/totp/setup/` (tidak terpengaruh user lain)
- ✅ User B: Bisa login normal, masuk `/totp/verify/`, lalu `/admin/`
- ✅ User C: Langsung bisa akses `/admin/` tanpa TOTP

**TIDAK BOLEH:** User B terblokir karena User A sedang setup TOTP

### Scenario 5: Reset Password oleh Superadmin
**User:** HRD dengan totp_enabled=True
**Action:** Superadmin klik "Reset Password"
**Expected:**
1. force_password_change=True
2. totp_enabled=True (masih enabled)
3. User login → Priority 1 (password change) → Priority 2 (TOTP verify) → Success

### Scenario 6: Reset TOTP oleh Superadmin
**User:** HRD dengan totp_enabled=True
**Action:** Superadmin klik "Reset TOTP"
**Expected:**
1. force_password_change=False (tidak berubah)
2. totp_enabled=False (direset)
3. User login → Priority 3 (TOTP setup) → Success

## Middleware Logic

```
if user.is_superuser:
    ✅ ALLOW - Skip semua check
    
else if hasattr(user, 'admin_account'):
    admin = user.admin_account
    
    if admin.force_password_change == True:
        🔄 REDIRECT → /admin/password_change/ (PRIORITY 1)
    
    else if admin.totp_enabled == True AND user.is_verified() == False:
        🔄 REDIRECT → /totp/verify/ (PRIORITY 2)
    
    else if admin.totp_enabled == False:
        🔄 REDIRECT → /totp/setup/ (PRIORITY 3)
    
    else:
        ✅ ALLOW - User sudah verified TOTP
        
else:
    ✅ ALLOW - Bukan admin account
```

## Priority Order (Important!)

1. **PRIORITY 1:** Force Password Change
   - Harus ganti password dulu sebelum apapun
   
2. **PRIORITY 2:** TOTP Verification
   - Jika TOTP sudah di-setup, harus verify setiap login
   
3. **PRIORITY 3:** TOTP Setup
   - Jika TOTP belum di-setup, harus setup dulu

## Key Points

✅ **Superadmin ALWAYS bypass** - Tidak kena middleware sama sekali
✅ **Per-user basis** - Setiap user punya session dan state sendiri
✅ **Session-based verification** - `user.is_verified()` check per session
✅ **Multiple users independent** - User A tidak mempengaruhi User B

## Test Commands

```bash
# Test 1: Login as superadmin
# Expected: Direct access to /admin/

# Test 2: Login as HRD (first time)
# Expected: /admin/password_change/ → /totp/setup/ → /admin/

# Test 3: Login as Direktur (already setup)
# Expected: /totp/verify/ → /admin/

# Test 4: Open 3 browsers, login as 3 different users
# Expected: Each user independent, no blocking
```

## Debugging

If User B blocked while User A doing setup:

Check:
1. ❌ User B has `force_password_change=True`?
2. ❌ User B has `totp_enabled=False`?
3. ❌ Session mixing (using same browser/cookies)?
4. ✅ Should use different browsers or incognito windows

## Fix Applied

```python
# ADDED: Skip middleware for superadmin
if request.user.is_superuser:
    return self.get_response(request)
```

This ensures:
- Superadmin never blocked
- HRD/Direktur handled individually
- No cross-user blocking
