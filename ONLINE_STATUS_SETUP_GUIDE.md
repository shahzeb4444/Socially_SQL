# 🟢 Online/Offline Status Feature - Setup Guide

## ✅ Implementation Complete!

The real-time online/offline status feature has been successfully implemented using PHP + MySQL!

---

## 📋 What Was Done:

### ✅ Android App (Already Applied):
1. **socialhomescreenchat.kt** - Added status polling every 5 seconds
2. **ApiModels.kt** - Added `GetUserStatusRequest` and `GetUserStatusResponse`
3. **ApiService.kt** - Added `getUserStatus()` endpoint

### ✅ PHP Backend Files Created:
1. **`update_status.php`** - Updates user's online status
2. **`get_user_status.php`** - Retrieves user's online status

### ✅ Database SQL Created:
1. **`ADD_ONLINE_STATUS_COLUMNS.sql`** - Adds required columns to users table

---

## 🚀 Setup Steps (5 Minutes):

### **STEP 1: Update Database** (2 min)

1. Open **phpMyAdmin**: `http://localhost/phpmyadmin`
2. Select database: **`socially_db`**
3. Click **SQL** tab
4. Run this SQL:

```sql
-- Add online status columns to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS is_online TINYINT(1) DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_seen BIGINT DEFAULT 0;

-- Add indexes for faster queries
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_is_online (is_online);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_last_seen (last_seen);

-- Verify columns were added
DESCRIBE users;
```

**Expected Result:**
```
Field      | Type        | Null | Key
-----------+-------------+------+-----
uid        | varchar(50) | NO   | PRI
username   | varchar(50) | NO   |
...
is_online  | tinyint(1)  | YES  | MUL  ← NEW!
last_seen  | bigint      | YES  | MUL  ← NEW!
```

---

### **STEP 2: Copy PHP Files** (1 min)

Copy these two files from your Android Studio project root to XAMPP:

**FROM:** `C:\Users\Dr Irum Shaikh\AndroidStudioProjects\23I-0610-23I-0040_Assignment3_Socially\`

**TO:** `C:\xampp\htdocs\socially_api\messages\`

**Files to copy:**
1. `update_status.php`
2. `get_user_status.php`

**Final location:**
```
C:\xampp\htdocs\socially_api\messages\
├── send_message.php
├── get_messages.php
├── poll_new_messages.php
├── update_status.php         ← NEW!
├── get_user_status.php        ← NEW!
├── mark_viewed.php
└── trigger_vanish.php
```

---

### **STEP 3: Test in App** (2 min)

1. **Build and run app** on two devices (or emulators)
2. **User A** - Login and open app
3. **User B** - Login and open chat with User A
4. **Check status**: User B should see "Online" in green at top of chat
5. **User A closes app** (swipe from recents)
6. **Wait 5-10 seconds**
7. **Check status**: User B should see "Offline" or "Last seen X ago" in gray

---

## 🎯 How It Works:

### **When User Opens Chat:**
```
User A opens chat with User B
    ↓
App calls updateOnlineStatus(true)
    ↓
PHP: UPDATE users SET is_online = 1, last_seen = [now]
    ↓
User B's app polls every 5 seconds
    ↓
PHP: SELECT is_online, last_seen FROM users WHERE uid = userA
    ↓
User B sees: "Online" (green text)
```

### **When User Closes App:**
```
User A closes app
    ↓
App calls updateOnlineStatus(false) in onDestroy()
    ↓
PHP: UPDATE users SET is_online = 0, last_seen = [now]
    ↓
User B's app polls after 5 seconds
    ↓
PHP: SELECT is_online, last_seen FROM users
    ↓
User B sees: "Last seen 1m ago" (gray text)
```

---

## 📊 Status Display Logic:

| Condition | Display | Color |
|-----------|---------|-------|
| is_online = 1 AND last_seen < 30s ago | "Online" | Green #4CAF50 |
| is_online = 0 AND last_seen < 1 min | "Last seen just now" | Gray #9E9E9E |
| is_online = 0 AND last_seen < 1 hour | "Last seen 15m ago" | Gray #9E9E9E |
| is_online = 0 AND last_seen < 1 day | "Last seen 3h ago" | Gray #9E9E9E |
| is_online = 0 AND last_seen < 1 week | "Last seen 2d ago" | Gray #9E9E9E |
| is_online = 0 AND last_seen > 1 week | "Last seen a while ago" | Gray #9E9E9E |

---

## 🔍 Database Schema:

### **users Table - New Columns:**

```sql
is_online  TINYINT(1) DEFAULT 0    -- 0 = offline, 1 = online
last_seen  BIGINT DEFAULT 0        -- Timestamp in milliseconds
```

### **Example Data:**

```
| uid                    | username | is_online | last_seen        | Status Display    |
|------------------------|----------|-----------|------------------|-------------------|
| usr_691db55d754ea5...  | Alice    | 1         | 1732691234567    | Online            |
| usr_69272f80bafb62...  | Bob      | 0         | 1732691134567    | Last seen 1m ago  |
```

---

## 🧪 Testing Checklist:

- [ ] Database columns added (run `DESCRIBE users;`)
- [ ] PHP files copied to XAMPP messages folder
- [ ] Apache running in XAMPP
- [ ] App built and installed on two devices
- [ ] User A opens app → User B sees "Online" in chat
- [ ] User A closes app → User B sees "Offline" after 5-10 seconds
- [ ] Status changes from green to gray
- [ ] "Last seen" shows correct time ago

---

## 🐛 Troubleshooting:

### Issue: Status always shows "Offline"

**Solutions:**
1. Check database has `is_online` and `last_seen` columns
2. Check PHP files are in correct location
3. Check error logs: `C:\xampp\apache\logs\error.log`
4. Check if `updateOnlineStatus()` is being called (add Log.d in Android)

### Issue: Status doesn't update

**Solutions:**
1. Make sure polling is working (check Logcat for "checkUserStatus")
2. Verify `otherUserId` is not empty
3. Check internet connection
4. Restart app

### Issue: Shows "Online" even when app is closed

**Solutions:**
1. `onDestroy()` might not be called - normal on some devices
2. PHP checks `last_seen` timestamp (if > 30s old, shows offline)
3. Wait 30 seconds for PHP to consider user offline

---

## 📝 SQL Verification Queries:

### **Check if columns exist:**
```sql
DESCRIBE users;
```

### **Check online users:**
```sql
SELECT 
    uid, 
    username, 
    is_online,
    FROM_UNIXTIME(last_seen/1000) as last_seen_time,
    TIMESTAMPDIFF(SECOND, FROM_UNIXTIME(last_seen/1000), NOW()) as seconds_ago
FROM users 
WHERE is_online = 1;
```

### **Manually set user online for testing:**
```sql
UPDATE users 
SET is_online = 1, last_seen = UNIX_TIMESTAMP() * 1000 
WHERE username = 'YourUsername';
```

### **Check when user was last online:**
```sql
SELECT 
    username,
    is_online,
    FROM_UNIXTIME(last_seen/1000) as last_online,
    TIMESTAMPDIFF(MINUTE, FROM_UNIXTIME(last_seen/1000), NOW()) as minutes_ago
FROM users 
ORDER BY last_seen DESC 
LIMIT 10;
```

---

## ✨ Features:

✅ **Real-time Status** - Updates every 5 seconds  
✅ **Automatic Updates** - Sets online when app opens, offline when closed  
✅ **Smart Display** - Shows "Online", "Offline", or "Last seen X ago"  
✅ **Color Coded** - Green for online, gray for offline  
✅ **Efficient** - Polls only when chat is open  
✅ **Battery Friendly** - 5-second interval (not too frequent)  
✅ **Accurate** - Uses server timestamp for consistency  

---

## 🎓 How Polling Works:

```kotlin
// In onCreate()
startStatusPolling()

// Polls every 5 seconds
private fun startStatusPolling() {
    statusPollHandler.postDelayed(object : Runnable {
        override fun run() {
            checkUserStatus()  // API call to get_user_status.php
            if (isStatusPolling) {
                statusPollHandler.postDelayed(this, 5000L)
            }
        }
    }, 5000L)
}

// In onDestroy()
isStatusPolling = false
statusPollHandler.removeCallbacksAndMessages(null)
updateOnlineStatus(false)  // Set self offline
```

---

## 🎉 You're Done!

Your app now shows real-time online/offline status just like WhatsApp, Facebook Messenger, and Instagram!

**Last Updated:** November 27, 2024  
**Status:** ✅ COMPLETE  
**Version:** 1.0

