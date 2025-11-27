# 🔧 DEBUGGING GUIDE - Message Notifications Not Working

## Issue: Notifications not being saved to database

---

## ✅ STEP 1: Replace send_message.php

**File Location:** `C:\xampp\htdocs\socially_api\messages\send_message.php`

**Action:** Copy the content from `CORRECTED_send_message.php` (in your project root) and replace your current `send_message.php`

### Key Changes in Corrected Version:
1. ✅ Fixed column order to match your database
2. ✅ Removed `created_at` from INSERT (it auto-fills)
3. ✅ Added extensive logging (every step)
4. ✅ Better error handling with PDO error info
5. ✅ Explicit column names in INSERT statement

---

## ✅ STEP 2: Run Database Diagnostic

1. Open **phpMyAdmin**: `http://localhost/phpmyadmin`
2. Select database: **`socially_db`**
3. Click **SQL** tab
4. Copy content from `DATABASE_DIAGNOSTIC.sql` (in your project root)
5. Paste and click **Go**

### Expected Results:

**Query 1 (DESCRIBE notifications):**
```
Field         | Type        | Null | Key
--------------+-------------+------+-----
id            | int         | NO   | PRI
user_id       | varchar(50) | NO   | 
from_user_id  | varchar(50) | NO   |
type          | varchar(50) | NO   |
title         | text        | NO   |
message       | text        | NO   |
data_json     | text        | YES  |
is_read       | tinyint(1)  | YES  |
created_at    | timestamp   | YES  |
```

**Query 2 (FCM Tokens):**
Should show at least one user with `token_status = 'EXISTS'`

**Query 6-7 (Test Insert):**
Should successfully insert a test notification

---

## ✅ STEP 3: Clear PHP Error Log

**Action:** Delete or clear the error log file

**Location:** `C:\xampp\apache\logs\error.log`

**How:**
1. Stop Apache in XAMPP
2. Delete or rename `error.log`
3. Start Apache
4. Fresh log file will be created

---

## ✅ STEP 4: Send Test Message

1. **Open Android app** - Login as User A
2. **Open chat** with User B
3. **Send message:** "Test message"
4. **Check if message appears** in chat

---

## ✅ STEP 5: Check Error Logs

**Open:** `C:\xampp\apache\logs\error.log`

**Search for these log entries:**

### ✅ SUCCESS Pattern:
```
=== MESSAGE SAVED ===
Message ID: msg_xxx
Chat ID: userA_userB
Sender: userA (UserA)
Text: Test message
Extracted receiver_id: 'userB' from chat_id: 'userA_userB'
Receiver FOUND in database
Receiver UID: userB
FCM Token: EXISTS (length: 163)
=== REGULAR MESSAGE ===
Notification Type: new_message
Title: New Message from UserA
Body: Test message
=== SAVING NOTIFICATION TO DATABASE ===
user_id: userB
from_user_id: userA
type: new_message
title: New Message from UserA
message: Test message
data_json: {"type":"new_message",...}
is_read: 0
SUCCESS: Notification saved to database with ID: 15
=== SENDING FCM NOTIFICATION ===
FCMHelper::sendNotification called
Token: xxxxx...
Title: New Message from UserA
Body: Test message
Getting OAuth access token...
Access token obtained successfully
Sending FCM request to: https://fcm.googleapis.com/...
FCM HTTP Response Code: 200
SUCCESS: Notification sent successfully
SUCCESS: FCM notification sent!
=== RETURNING SUCCESS RESPONSE ===
```

### ❌ FAILURE Patterns & Solutions:

#### Pattern 1: "Receiver NOT found in database"
```
ERROR: Receiver NOT found in database for uid: 'userB'
```
**Solution:** Check that User B exists in `users` table with correct `uid`

#### Pattern 2: "Receiver has no FCM token"
```
SKIP: Receiver has no FCM token
```
**Solution:** User B must login at least once to register FCM token

#### Pattern 3: "FAILED to save notification to database"
```
FAILED to save notification to database
PDO Error: Array(...)
```
**Solution:** Check the PDO error details - likely column mismatch

#### Pattern 4: "Could not extract receiver_id"
```
ERROR: Could not extract receiver_id from chat_id
```
**Solution:** Check chat_id format - should be "userA_userB"

---

## ✅ STEP 6: Verify Database

**Run this SQL:**
```sql
SELECT * FROM notifications ORDER BY id DESC LIMIT 1;
```

**Expected Result:**
```
| id | user_id | from_user_id | type        | title                  | message      | data_json | is_read | created_at |
|----|---------|--------------|-------------|------------------------|--------------|-----------|---------|------------|
| 15 | userB   | userA        | new_message | New Message from UserA | Test message | {...}     | 0       | 2024-11-27 |
```

**If notification EXISTS in database but NO push received:**
- Issue is with FCM, not database
- Check FCM token is valid
- Check `firebase-service-account.json` exists
- Check device has internet

**If notification DOES NOT EXIST in database:**
- Check error logs for PDO errors
- Run diagnostic SQL to test manual insert
- Check column names match exactly

---

## ✅ STEP 7: Test Screenshot Notification

1. User A opens chat with User B
2. User A takes screenshot
3. Check logs for: `=== SCREENSHOT MESSAGE DETECTED ===`
4. Check database for notification with `type = 'screenshot'`

---

## 🔍 Common Issues & Fixes

### Issue 1: "created_at" column error
```
PDO Error: Unknown column 'created_at' in 'field list'
```
**Solution:** Your table uses `timestamp` column instead
```sql
ALTER TABLE notifications CHANGE timestamp created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

### Issue 2: Column order mismatch
```
PDO Error: Column count doesn't match value count
```
**Solution:** The corrected PHP explicitly names columns, so this shouldn't happen

### Issue 3: Data too long for column
```
PDO Error: Data too long for column 'data_json'
```
**Solution:** Change column type:
```sql
ALTER TABLE notifications MODIFY COLUMN data_json TEXT;
```

### Issue 4: NULL values not allowed
```
PDO Error: Column 'xxx' cannot be null
```
**Solution:** Check which column and make it nullable or provide default

---

## 📊 Quick Checklist

Before contacting support, verify:

- [ ] `send_message.php` replaced with corrected version
- [ ] Apache and MySQL running in XAMPP
- [ ] Database diagnostic SQL runs without errors
- [ ] Test notification can be manually inserted
- [ ] Error log cleared and shows new entries
- [ ] Test message sent through app
- [ ] Error log checked for success/failure pattern
- [ ] Database checked for notification entry
- [ ] Both users have FCM tokens in database

---

## 🎯 Expected Flow

```
User A sends "Hello!" to User B
    ↓
PHP: Message saved to messages table ✅
    ↓
PHP: Extracted receiver_id = "userB" ✅
    ↓
PHP: Found userB in database ✅
    ↓
PHP: userB has FCM token ✅
    ↓
PHP: Created notification data ✅
    ↓
PHP: Saved to notifications table ✅
    ↓
PHP: Sent FCM push ✅
    ↓
User B's device: Push notification received! 🎉
```

---

## 📞 What to Share if Still Not Working

If it's still not working after following all steps, share:

1. **Error log entries** from `C:\xampp\apache\logs\error.log` (after sending test message)
2. **Result of DESCRIBE notifications** SQL query
3. **Result of SELECT * FROM notifications** query
4. **Result of test manual INSERT** from diagnostic SQL
5. **Chat ID format** - what does it look like in database?

---

**Good luck! Follow the steps in order and check the logs carefully.**

