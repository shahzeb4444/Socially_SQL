# 🚀 Quick Start - Testing Your PHP + MySQL Login/Signup

## ⚡ 5-Minute Setup

### 1️⃣ Start XAMPP (30 seconds)
```
Open XAMPP → Start Apache → Start MySQL
```

### 2️⃣ Create Database (1 minute)
```
1. Go to: http://localhost/phpmyadmin
2. Click "New" → Database name: socially_db → Create
3. Click socially_db → SQL tab → Paste your CREATE TABLE query → Go
```

### 3️⃣ Create PHP Files (2 minutes)

**Create folder structure:**
```
C:\xampp\htdocs\socially_api\
    config\Database.php
    models\User.php
    endpoints\auth\register.php
    endpoints\auth\login.php
```

**Copy code from `SETUP_GUIDE.md` → Save each file**

### 4️⃣ Update Android IP (30 seconds)

Open: `network/RetrofitInstance.kt`

**For Emulator:**
```kotlin
private const val BASE_URL = "http://10.0.2.2/socially_api/endpoints/"
```

**For Physical Device:**
```
1. Open CMD → type: ipconfig
2. Find IPv4 Address (e.g., 192.168.1.100)
3. Update:
private const val BASE_URL = "http://192.168.1.100/socially_api/endpoints/"
```

### 5️⃣ Test (1 minute)

**Test PHP in Browser:**
```
http://localhost/socially_api/endpoints/auth/login.php
```
Should see: JSON error message (means it's working!)

**Run Android App:**
```
Build → Run → Try signup → Check database
```

---

## 🧪 Testing Endpoints with Postman

### Test Registration:
```
Method: POST
URL: http://localhost/socially_api/endpoints/auth/register.php
Headers: Content-Type: application/json
Body (raw JSON):
{
    "email": "john@example.com",
    "password": "test123",
    "username": "johndoe",
    "full_name": "John Doe"
}

Expected Response:
{
    "success": true,
    "data": {
        "user": {
            "id": "1",
            "uid": "usr_...",
            "email": "john@example.com",
            "username": "johndoe",
            "full_name": "John Doe",
            ...
        }
    }
}
```

### Test Login:
```
Method: POST
URL: http://localhost/socially_api/endpoints/auth/login.php
Headers: Content-Type: application/json
Body (raw JSON):
{
    "email": "john@example.com",
    "password": "test123"
}

Expected Response: Same as registration
```

---

## 🐛 Common Issues & Fixes

### ❌ "Network error" in app
```
✅ Fix:
1. Check XAMPP Apache is running
2. Verify IP address in RetrofitInstance.kt
3. Disable Windows Firewall temporarily
```

### ❌ "Connection refused"
```
✅ Fix:
- Emulator: Use 10.0.2.2 not localhost
- Device: Use PC IP (192.168.x.x)
- Check both are on same WiFi
```

### ❌ PHP file downloads instead of running
```
✅ Fix:
1. Make sure Apache is running
2. Access via localhost not file path
3. Check .php extension is correct
```

### ❌ "Email already exists"
```
✅ Fix:
1. Go to phpMyAdmin
2. Browse users table
3. Delete test user OR use different email
```

### ❌ Can't find PC IP
```
✅ Fix (Windows):
1. Open CMD
2. Type: ipconfig
3. Look for "IPv4 Address" under your WiFi adapter
4. Use that IP (e.g., 192.168.1.100)
```

---

## 📱 Android App Flow

### Signup Flow:
```
1. User fills form in second_page.kt
2. Clicks "Create Account"
3. App validates locally
4. Sends HTTP POST to register.php
5. PHP creates user in MySQL
6. Returns user data
7. App saves to SharedPreferences
8. Navigates to login_splash.kt
```

### Login Flow:
```
1. User enters email/password in mainlogin.kt
2. Clicks "Login"
3. App validates locally
4. Sends HTTP POST to login.php
5. PHP verifies password
6. Returns user data
7. App saves to SharedPreferences
8. Navigates to login_splash.kt
```

### Session Check:
```
1. App starts → login_splash.kt loads
2. Checks UserPreferences.isLoggedIn()
3. If true: Shows profile splash
4. If false: Redirects to mainlogin.kt
```

---

## 📊 Check If Everything Works

### Checklist:
- [ ] XAMPP Apache & MySQL green in control panel
- [ ] Database `socially_db` exists in phpMyAdmin
- [ ] Can see `users` table in database
- [ ] Can access login.php in browser (shows JSON error)
- [ ] IP address correct in RetrofitInstance.kt
- [ ] App builds without errors
- [ ] Signup creates user (check phpMyAdmin)
- [ ] Login works with test user
- [ ] Logout clears session

### Verify in Database:
```
1. Go to phpMyAdmin
2. Click socially_db → users table
3. Click "Browse"
4. Should see registered users
5. Password should be hashed (not plain text)
```

---

## 🎯 What Happens When You Start XAMPP & Run App

### Backend (XAMPP):
```
Apache Server (Port 80)
    ├── Hosts PHP files
    ├── Processes HTTP requests
    └── Returns JSON responses

MySQL Server (Port 3306)
    ├── Stores user data
    ├── Handles queries from PHP
    └── Returns results
```

### Android App:
```
User Action (Login/Signup)
    ↓
ViewModel processes
    ↓
Repository makes HTTP call
    ↓
Retrofit sends JSON to PHP
    ↓
PHP queries MySQL
    ↓
MySQL returns data
    ↓
PHP sends JSON response
    ↓
Retrofit parses JSON
    ↓
ViewModel updates LiveData
    ↓
Activity observes & updates UI
```

---

## 💡 Pro Tips

1. **Always check Apache & MySQL are running first**
2. **Use Postman to test PHP before testing app**
3. **Check Android Logcat for error messages**
4. **Check XAMPP Apache error logs if PHP fails**
5. **Use phpMyAdmin to verify data is saved**

---

## 📞 Need Help?

### Check These in Order:
1. XAMPP logs: `C:\xampp\apache\logs\error.log`
2. Android Logcat: Look for Retrofit/OkHttp errors
3. phpMyAdmin: Check if users table exists
4. Browser: Test login.php directly
5. Network: Ping PC from phone

### Enable Verbose Logging:

**Android (Already enabled):**
```kotlin
// RetrofitInstance.kt
HttpLoggingInterceptor.Level.BODY // Shows all request/response
```

**PHP (Add to login.php/register.php):**
```php
error_reporting(E_ALL);
ini_set('display_errors', 1);
```

---

**You're ready to test! Start XAMPP and run your app! 🚀**

