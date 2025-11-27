-- =====================================================
-- DIAGNOSTIC SQL SCRIPT
-- Run this in phpMyAdmin to check your database setup
-- =====================================================

-- 1. Check notifications table structure
DESCRIBE notifications;

-- 2. Check if users have FCM tokens
SELECT
    uid,
    SUBSTRING(fcm_token, 1, 30) as fcm_token_preview,
    CASE
        WHEN fcm_token IS NULL THEN 'NULL'
        WHEN fcm_token = '' THEN 'EMPTY'
        ELSE 'EXISTS'
    END as token_status
FROM users
ORDER BY uid;

-- 3. Check recent messages
SELECT
    message_id,
    chat_id,
    sender_id,
    sender_username,
    SUBSTRING(text, 1, 50) as text_preview,
    FROM_UNIXTIME(timestamp/1000) as sent_at,
    is_vanish_mode
FROM messages
ORDER BY timestamp DESC
LIMIT 5;

-- 4. Check if notifications table exists and has data
SELECT COUNT(*) as total_notifications FROM notifications;

-- 5. Check recent notifications (if any)
SELECT
    id,
    user_id,
    from_user_id,
    type,
    title,
    message,
    is_read,
    created_at
FROM notifications
ORDER BY id DESC
LIMIT 5;

-- 6. Test manual notification insert (to verify table structure)
-- IMPORTANT: Replace 'test_user_id' with an actual user ID from your users table
INSERT INTO notifications
(user_id, from_user_id, type, title, message, data_json, is_read)
VALUES
('test_user_id', 'test_sender_id', 'new_message', 'Test Title', 'Test Message', '{"test":"data"}', 0);

-- 7. Check if the test notification was inserted
SELECT * FROM notifications WHERE title = 'Test Title';

-- 8. Delete the test notification
DELETE FROM notifications WHERE title = 'Test Title';

-- =====================================================
-- EXPECTED RESULTS:
-- =====================================================
-- Query 1: Should show columns in this order:
--   id, user_id, from_user_id, type, title, message, data_json, is_read, created_at
--
-- Query 2: Should show users with their FCM token status
--   At least one user should have 'EXISTS' status
--
-- Query 3: Should show recent messages
--
-- Query 5: Should show recent notifications
--   If empty, notifications are not being saved
--
-- Query 6-7: Should successfully insert and retrieve test notification
--   If this fails, there's a database structure problem
-- =====================================================

