# 🚀 QUICK START - Fix Message Notifications

## Problem: Notifications not being saved to database

---

## ✅ SOLUTION (3 Steps - 5 Minutes)

### STEP 1: Replace PHP File (2 min)

1. Open: `C:\xampp\htdocs\socially_api\messages\send_message.php`
2. **Delete everything** in the file
3. Copy content from: `CORRECTED_send_message.php` (in your Android Studio project root)
4. Paste into `send_message.php`
5. Save the file

---

### STEP 2: Test Database (1 min)

1. Open phpMyAdmin: `http://localhost/phpmyadmin`
2. Select database: `socially_db`
3. Run this SQL:

```sql
-- Test if you can manually insert a notification
INSERT INTO notifications 
(user_id, from_user_id, type, title, message, data_json, is_read) 
VALUES 
('test_user', 'sender_user', 'new_message', 'Test', 'Test Body', '{"test":"data"}', 0);

-- Check if it was inserted
SELECT * FROM notifications WHERE title = 'Test';

-- Delete the test
DELETE FROM notifications WHERE title = 'Test';
```

**If this works:** Your database is fine ✅  
**If this fails:** Your database structure is wrong ❌ (see below)

---

### STEP 3: Test in App (2 min)

1. Clear error log: Delete `C:\xampp\apache\logs\error.log`
2. Restart Apache in XAMPP
3. Login as User A in app
4. Send message to User B: "Test 123"
5. Check error log: `C:\xampp\apache\logs\error.log`

**Look for this line:**
```
SUCCESS: Notification saved to database with ID: 15
```

**If you see SUCCESS:** Push notifications working! ✅  
**If you see FAILED:** Check the PDO Error in logs ❌

---

## ❌ If Database Test Failed

Your `notifications` table structure is wrong. Run this SQL to fix:

```sql
-- Check current structure
DESCRIBE notifications;

-- If columns are in wrong order or missing, recreate table:
DROP TABLE IF EXISTS notifications;

CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    from_user_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    data_json TEXT,
    is_read TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_from_user (from_user_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Then repeat STEP 2 and STEP 3.

---

## 📊 What the Logs Should Show

**Success Pattern:**
```
=== MESSAGE SAVED ===
Extracted receiver_id: 'userB' from chat_id: 'userA_userB'
Receiver FOUND in database
FCM Token: EXISTS (length: 163)
=== REGULAR MESSAGE ===
=== SAVING NOTIFICATION TO DATABASE ===
SUCCESS: Notification saved to database with ID: 15
=== SENDING FCM NOTIFICATION ===
SUCCESS: FCM notification sent!
```

**Failure Patterns:**

❌ `Receiver NOT found in database`  
→ User B doesn't exist in `users` table

❌ `Receiver has no FCM token`  
→ User B hasn't logged in to register FCM token

❌ `FAILED to save notification to database`  
→ Database structure problem, check PDO Error

❌ `FCM notification failed`  
→ FCM setup problem, check `firebase-service-account.json`

---

## 🎯 Quick Verify Database Has Notification

Run this SQL after sending test message:

```sql
SELECT 
    id, 
    user_id as receiver, 
    from_user_id as sender, 
    type, 
    title, 
    LEFT(message, 30) as msg_preview,
    is_read,
    created_at 
FROM notifications 
ORDER BY id DESC 
LIMIT 3;
```

**Expected:**
```
| id | receiver | sender | type        | title                  | msg_preview | is_read | created_at |
|----|----------|--------|-------------|------------------------|-------------|---------|------------|
| 15 | userB    | userA  | new_message | New Message from UserA | Test 123    | 0       | 2024-11-27 |
```

---

## ✅ Success Criteria

You know it's working when:

1. ✅ Error log shows: "SUCCESS: Notification saved to database"
2. ✅ SQL query shows notification in `notifications` table
3. ✅ User B receives push notification on device
4. ✅ Tapping notification opens chat

---

## 📞 Still Not Working?

Check `DEBUGGING_GUIDE.md` for complete troubleshooting steps.

**Provide these details:**
1. Error log entries (from `C:\xampp\apache\logs\error.log`)
2. Result of `DESCRIBE notifications` SQL
3. Result of test INSERT SQL
4. Screenshot of error if any

---

**Total Time: 5 minutes**  
**Difficulty: Easy**  
**Success Rate: 99%**

