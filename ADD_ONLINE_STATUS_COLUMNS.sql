-- =====================================================
-- ADD ONLINE STATUS COLUMNS TO USERS TABLE
-- Run this in phpMyAdmin
-- =====================================================

-- Check if columns exist
DESCRIBE users;

-- Add is_online and last_seen columns if they don't exist
ALTER TABLE users
ADD COLUMN IF NOT EXISTS is_online TINYINT(1) DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_seen BIGINT DEFAULT 0;

-- Add index for faster queries
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_is_online (is_online);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_last_seen (last_seen);

-- Verify columns were added
DESCRIBE users;

-- Test: Set a user online
UPDATE users
SET is_online = 1, last_seen = UNIX_TIMESTAMP() * 1000
WHERE uid = (SELECT uid FROM (SELECT uid FROM users LIMIT 1) AS temp)
LIMIT 1;

-- Test: Check online users
SELECT uid, username, is_online, last_seen, FROM_UNIXTIME(last_seen/1000) as last_seen_time
FROM users
WHERE is_online = 1;

