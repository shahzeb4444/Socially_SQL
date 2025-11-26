# Story Feature Implementation Guide

## Overview
This guide documents the complete implementation of the Instagram-style Stories feature using PHP + MySQL backend instead of Firebase. The implementation includes story creation, viewing, deletion, and proper navigation between stories.

---

## Table of Contents
1. [Backend PHP Files](#backend-php-files)
2. [Android Kotlin Changes](#android-kotlin-changes)
3. [How It Works](#how-it-works)
4. [App Flow](#app-flow)
5. [Testing Instructions](#testing-instructions)

---

## Backend PHP Files

### 1. Create Story Endpoint
**File:** `C:\xampp\htdocs\socially_api\endpoints\stories\create.php`

```php
<?php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../models/Story.php';
require_once __DIR__ . '/../../models/User.php';
require_once __DIR__ . '/../../utils/response.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::error('Method not allowed. Use POST', 405);
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        Response::error('Invalid JSON format', 400);
    }

    if (empty($data)) {
        Response::error('Request body is empty', 400);
    }

    $database = new Database();
    $db = $database->getConnection();
    $story = new Story($db);
    $userModel = new User($db);

    // Required fields
    $story->story_id = isset($data['story_id']) ? trim($data['story_id']) : '';
    $story->user_id = isset($data['user_id']) ? trim($data['user_id']) : '';
    $story->image_base64 = isset($data['image_base64']) ? trim($data['image_base64']) : '';

    // Validate required fields
    if (empty($story->story_id) || empty($story->user_id) || empty($story->image_base64)) {
        Response::error('Story ID, user ID, and image are required', 400);
    }

    // Verify user exists
    $user = $userModel->findByUID($story->user_id);
    if (!$user) {
        Response::error('User not found', 404);
    }

    // Set user info from database
    $story->username = $user['username'];
    $story->user_photo_base64 = $user['profile_image_url'] ?? null;

    // Timestamps
    $story->timestamp = isset($data['timestamp']) ? intval($data['timestamp']) : intval(microtime(true) * 1000);
    $story->expires_at = isset($data['expires_at']) ? intval($data['expires_at']) : ($story->timestamp + (24 * 60 * 60 * 1000));

    // Close friends flag
    $story->is_close_friends_only = isset($data['is_close_friends_only']) ? (bool)$data['is_close_friends_only'] : false;

    // Create story
    $result = $story->create();

    if ($result['success']) {
        Response::success($result['data'], 201);
    } else {
        Response::error($result['error'], 400);
    }

} catch (Exception $e) {
    Response::error('Failed to create story: ' . $e->getMessage(), 500);
}
```

---

### 2. Get Feed Stories Endpoint
**File:** `C:\xampp\htdocs\socially_api\endpoints\stories\get_feed.php`

```php
<?php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../utils/response.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::error('Method not allowed. Use POST', 405);
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        Response::error('Invalid JSON format', 400);
    }

    if (empty($data)) {
        Response::error('Request body is empty', 400);
    }

    $userId = isset($data['user_id']) ? trim($data['user_id']) : '';

    if (empty($userId)) {
        Response::error('User ID is required', 400);
    }

    $database = new Database();
    $db = $database->getConnection();

    $currentTime = time() * 1000; // Convert to milliseconds

    // Get current user's info to ensure they're always in the list
    $userQuery = "SELECT uid, username, profile_image_url FROM users WHERE uid = :user_id";
    $userStmt = $db->prepare($userQuery);
    $userStmt->bindParam(':user_id', $userId);
    $userStmt->execute();
    $currentUserInfo = $userStmt->fetch(PDO::FETCH_ASSOC);

    // Get all active stories (not expired) grouped by user
    $query = "SELECT
                s.story_id,
                s.user_id,
                s.image_base64,
                s.timestamp,
                s.expires_at,
                s.is_close_friends_only,
                u.username,
                u.profile_image_url as user_photo_base64
              FROM stories s
              LEFT JOIN users u ON s.user_id = u.uid
              WHERE s.expires_at > :current_time
              ORDER BY s.user_id, s.timestamp ASC";

    $stmt = $db->prepare($query);
    $stmt->bindParam(':current_time', $currentTime, PDO::PARAM_INT);
    $stmt->execute();

    $storiesByUser = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Get views for this story from story_views table
        $viewsQuery = "SELECT viewer_user_id FROM story_views WHERE story_id = :story_id";
        $viewsStmt = $db->prepare($viewsQuery);
        $viewsStmt->bindParam(':story_id', $row['story_id']);
        $viewsStmt->execute();

        $viewedBy = [];
        while ($viewRow = $viewsStmt->fetch(PDO::FETCH_ASSOC)) {
            $viewedBy[$viewRow['viewer_user_id']] = true;
        }

        $story = [
            'story_id' => $row['story_id'],
            'user_id' => $row['user_id'],
            'username' => $row['username'] ?? 'Unknown',
            'user_photo_base64' => $row['user_photo_base64'],
            'image_base64' => $row['image_base64'] ?? '',
            'timestamp' => (int)$row['timestamp'],
            'expires_at' => (int)$row['expires_at'],
            'viewedBy' => (object)$viewedBy,
            'is_close_friends_only' => (bool)($row['is_close_friends_only'] ?? false)
        ];

        $userIdKey = $row['user_id'];
        if (!isset($storiesByUser[$userIdKey])) {
            $storiesByUser[$userIdKey] = [
                'userId' => $row['user_id'],
                'username' => $row['username'] ?? 'Unknown',
                'userPhotoBase64' => $row['user_photo_base64'],
                'stories' => []
            ];
        }

        $storiesByUser[$userIdKey]['stories'][] = $story;
    }

    // Ensure current user is always in the list (even with no stories)
    if ($currentUserInfo && !isset($storiesByUser[$userId])) {
        $storiesByUser[$userId] = [
            'userId' => $currentUserInfo['uid'],
            'username' => $currentUserInfo['username'],
            'userPhotoBase64' => $currentUserInfo['profile_image_url'],
            'stories' => []
        ];
    }

    // Convert to indexed array with current user first
    $storyGroups = [];
    if (isset($storiesByUser[$userId])) {
        $storyGroups[] = $storiesByUser[$userId];
        unset($storiesByUser[$userId]);
    }
    $storyGroups = array_merge($storyGroups, array_values($storiesByUser));

    Response::success([
        'story_groups' => $storyGroups
    ], 200);

} catch (Exception $e) {
    Response::error('Failed to fetch stories: ' . $e->getMessage(), 500);
}
```

---

### 3. Get User Stories Endpoint
**File:** `C:\xampp\htdocs\socially_api\endpoints\stories\get_user_stories.php`

```php
<?php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../utils/response.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::error('Method not allowed. Use POST', 405);
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        Response::error('Invalid JSON format', 400);
    }

    if (empty($data)) {
        Response::error('Request body is empty', 400);
    }

    $userId = isset($data['user_id']) ? trim($data['user_id']) : '';
    $viewerId = isset($data['viewer_id']) ? trim($data['viewer_id']) : '';

    if (empty($userId)) {
        Response::error('User ID is required', 400);
    }

    $database = new Database();
    $db = $database->getConnection();

    $currentTime = time() * 1000; // Convert to milliseconds

    // Get user's active stories (not expired)
    $query = "SELECT
                s.story_id,
                s.user_id,
                s.image_base64,
                s.timestamp,
                s.expires_at,
                s.is_close_friends_only,
                u.username,
                u.profile_image_url as user_photo_base64
              FROM stories s
              LEFT JOIN users u ON s.user_id = u.uid
              WHERE s.user_id = :user_id AND s.expires_at > :current_time
              ORDER BY s.timestamp ASC";

    $stmt = $db->prepare($query);
    $stmt->bindParam(':user_id', $userId);
    $stmt->bindParam(':current_time', $currentTime, PDO::PARAM_INT);
    $stmt->execute();

    $stories = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Get views for this story
        $viewsQuery = "SELECT viewer_user_id FROM story_views WHERE story_id = :story_id";
        $viewsStmt = $db->prepare($viewsQuery);
        $viewsStmt->bindParam(':story_id', $row['story_id']);
        $viewsStmt->execute();

        $viewedBy = [];
        while ($viewRow = $viewsStmt->fetch(PDO::FETCH_ASSOC)) {
            $viewedBy[$viewRow['viewer_user_id']] = true;
        }

        $story = [
            'story_id' => $row['story_id'],
            'user_id' => $row['user_id'],
            'username' => $row['username'] ?? 'Unknown',
            'user_photo_base64' => $row['user_photo_base64'],
            'image_base64' => $row['image_base64'] ?? '',
            'timestamp' => (int)$row['timestamp'],
            'expires_at' => (int)$row['expires_at'],
            'viewedBy' => (object)$viewedBy,
            'is_close_friends_only' => (bool)($row['is_close_friends_only'] ?? false)
        ];

        $stories[] = $story;
    }

    Response::success([
        'stories' => $stories
    ], 200);

} catch (Exception $e) {
    Response::error('Failed to fetch user stories: ' . $e->getMessage(), 500);
}
```

---

### 4. Mark Story Viewed Endpoint
**File:** `C:\xampp\htdocs\socially_api\endpoints\stories\mark_viewed.php`

```php
<?php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../utils/response.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::error('Method not allowed. Use POST', 405);
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        Response::error('Invalid JSON format', 400);
    }

    if (empty($data)) {
        Response::error('Request body is empty', 400);
    }

    $storyId = isset($data['story_id']) ? trim($data['story_id']) : '';
    $viewerId = isset($data['viewer_id']) ? trim($data['viewer_id']) : '';

    if (empty($storyId) || empty($viewerId)) {
        Response::error('Story ID and Viewer ID are required', 400);
    }

    $database = new Database();
    $db = $database->getConnection();

    // Check if story exists
    $storyQuery = "SELECT story_id FROM stories WHERE story_id = :story_id";
    $storyStmt = $db->prepare($storyQuery);
    $storyStmt->bindParam(':story_id', $storyId);
    $storyStmt->execute();

    $story = $storyStmt->fetch(PDO::FETCH_ASSOC);

    if (!$story) {
        Response::error('Story not found', 404);
    }

    // Check if already viewed
    $checkQuery = "SELECT * FROM story_views WHERE story_id = :story_id AND viewer_user_id = :viewer_id";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(':story_id', $storyId);
    $checkStmt->bindParam(':viewer_id', $viewerId);
    $checkStmt->execute();

    $existingView = $checkStmt->fetch(PDO::FETCH_ASSOC);

    // Add view if not already viewed
    if (!$existingView) {
        $insertQuery = "INSERT INTO story_views (story_id, viewer_user_id, viewed_at)
                        VALUES (:story_id, :viewer_id, NOW())";
        $insertStmt = $db->prepare($insertQuery);
        $insertStmt->bindParam(':story_id', $storyId);
        $insertStmt->bindParam(':viewer_id', $viewerId);
        $insertStmt->execute();
    }

    Response::success([
        'message' => 'Story marked as viewed'
    ], 200);

} catch (Exception $e) {
    Response::error('Failed to mark story as viewed: ' . $e->getMessage(), 500);
}
```

---

### 5. Delete Story Endpoint
**File:** `C:\xampp\htdocs\socially_api\endpoints\stories\delete.php`

```php
<?php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../utils/response.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::error('Method not allowed. Use POST', 405);
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        Response::error('Invalid JSON format', 400);
    }

    if (empty($data)) {
        Response::error('Request body is empty', 400);
    }

    $storyId = isset($data['story_id']) ? trim($data['story_id']) : '';
    $userId = isset($data['user_id']) ? trim($data['user_id']) : '';

    if (empty($storyId) || empty($userId)) {
        Response::error('Story ID and User ID are required', 400);
    }

    $database = new Database();
    $db = $database->getConnection();

    // Verify the story belongs to this user
    $verifyQuery = "SELECT user_id FROM stories WHERE story_id = :story_id";
    $verifyStmt = $db->prepare($verifyQuery);
    $verifyStmt->bindParam(':story_id', $storyId);
    $verifyStmt->execute();

    $story = $verifyStmt->fetch(PDO::FETCH_ASSOC);

    if (!$story) {
        Response::error('Story not found', 404);
    }

    if ($story['user_id'] !== $userId) {
        Response::error('Unauthorized to delete this story', 403);
    }

    // Delete views first (foreign key constraint)
    $deleteViewsQuery = "DELETE FROM story_views WHERE story_id = :story_id";
    $deleteViewsStmt = $db->prepare($deleteViewsQuery);
    $deleteViewsStmt->bindParam(':story_id', $storyId);
    $deleteViewsStmt->execute();

    // Delete the story
    $deleteQuery = "DELETE FROM stories WHERE story_id = :story_id";
    $deleteStmt = $db->prepare($deleteQuery);
    $deleteStmt->bindParam(':story_id', $storyId);

    if ($deleteStmt->execute()) {
        Response::success([
            'message' => 'Story deleted successfully',
            'story_id' => $storyId
        ], 200);
    } else {
        Response::error('Failed to delete story', 400);
    }

} catch (Exception $e) {
    Response::error('Failed to delete story: ' . $e->getMessage(), 500);
}
```

---

### 6. Cleanup Expired Stories Endpoint
**File:** `C:\xampp\htdocs\socially_api\endpoints\stories\cleanup_expired.php`

```php
<?php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../utils/response.php';

try {
    $database = new Database();
    $db = $database->getConnection();

    $currentTime = time() * 1000;

    $deleteQuery = "DELETE FROM stories WHERE expires_at <= :current_time";
    $deleteStmt = $db->prepare($deleteQuery);
    $deleteStmt->bindParam(':current_time', $currentTime, PDO::PARAM_INT);
    $deleteStmt->execute();

    $deletedCount = $deleteStmt->rowCount();

    Response::success([
        'message' => 'Expired stories cleaned up',
        'deleted_count' => $deletedCount
    ], 200);

} catch (Exception $e) {
    Response::error('Failed to cleanup expired stories: ' . $e->getMessage(), 500);
}
```

---

## Android Kotlin Changes

### Changes Already Made (No Action Required)

The following files have already been updated:

1. **ApiService.kt** - Added new API endpoints
2. **ApiModels.kt** - Added request/response models
3. **StoryRepository.kt** - Added repository methods
4. **StoryViewModel.kt** - Added ViewModel methods
5. **socialhomescreen14.kt** - Updated to use API instead of Firebase (Your Stories)
6. **socialhomescreen12.kt** - Updated to use API instead of Firebase (Other Users' Stories)

---

## How It Works

### 1. Story Creation Flow

```
User Interface (socialhomescreen15.kt)
    ↓
User selects image from gallery
    ↓
Image converted to Base64
    ↓
StoryViewModel.createStory()
    ↓
StoryRepository.createStory()
    ↓
API Call: POST /stories/create.php
    ↓
PHP validates user exists
    ↓
Story saved to MySQL database
    ↓
Response returned to app
    ↓
User redirected to HomeActivity
```

**Key Points:**
- Images are stored as Base64 in the database
- Stories automatically expire after 24 hours
- User info (username, profile photo) is fetched from users table
- Story ID is generated using timestamp + random number

---

### 2. Story Viewing Flow (Your Stories)

```
User clicks on their story icon in HomeFragment
    ↓
Opens socialhomescreen14 activity
    ↓
StoryViewModel.getUserStories(userId, viewerId)
    ↓
API Call: POST /stories/get_user_stories.php
    ↓
PHP fetches user's active stories + view counts
    ↓
Stories displayed with auto-progress (5 seconds each)
    ↓
User can:
  - Navigate left/right between stories
  - Delete current story
  - Close and return to home
```

**Key Points:**
- Only user's own stories are shown
- Delete button is visible
- Stories auto-advance every 5 seconds
- Progress bars show position in story sequence

---

### 3. Story Viewing Flow (Other Users' Stories)

```
User clicks on another user's story in HomeFragment
    ↓
Opens socialhomescreen12 activity
    ↓
StoryViewModel.getFeedStories(userId)
    ↓
API Call: POST /stories/get_feed.php
    ↓
PHP fetches all active stories grouped by user
    ↓
Stories reordered to start with clicked user
    ↓
Stories displayed with auto-progress
    ↓
As each story is viewed:
  - StoryViewModel.markStoryViewed(storyId, viewerId)
  - API Call: POST /stories/mark_viewed.php
  - View recorded in story_views table
    ↓
User can:
  - Navigate left/right through current user's stories
  - Auto-advance to next user's stories
  - Close and return to home
```

**Key Points:**
- Shows stories from all users with active stories
- Starts with the clicked user's stories
- Auto-advances through multiple users
- Views are tracked for each story
- No delete button for other users' stories

---

### 4. Story Deletion Flow

```
User viewing their own story
    ↓
Clicks delete button
    ↓
Confirmation dialog appears
    ↓
User confirms deletion
    ↓
StoryViewModel.deleteStory(storyId, userId)
    ↓
API Call: POST /stories/delete.php
    ↓
PHP verifies story ownership
    ↓
Deletes story views from story_views table
    ↓
Deletes story from stories table
    ↓
Success response returned
    ↓
Story removed from local list
    ↓
Next story displayed or user redirected to home
```

**Key Points:**
- Only story owner can delete
- Views are deleted first (foreign key constraint)
- Local story list is updated immediately
- User stays in viewer if more stories exist

---

## App Flow

### Complete User Journey

#### Scenario 1: Creating a Story

1. User opens app → HomeFragment displays
2. User sees their profile icon with "+" indicator in stories row
3. User clicks on their icon → socialhomescreen15 opens
4. User clicks on image placeholder → Gallery picker opens
5. User selects image → Image displayed in preview
6. User clicks "Your Story" or "Close Friends" button
7. API call made to create story
8. Story saved to database with 24-hour expiration
9. Success toast shown
10. User redirected to HomeFragment
11. Story now visible in stories row with filled ring

#### Scenario 2: Viewing Your Own Stories

1. User clicks on their story icon in HomeFragment
2. socialhomescreen14 opens
3. API fetches user's active stories
4. First story displays with:
   - Story image filling screen
   - Profile photo in top left
   - "Your Story" text
   - Progress bars showing position
   - Delete button in top right
   - Close button
5. Story auto-advances after 5 seconds
6. User can:
   - Tap left side → Previous story
   - Tap right side → Next story
   - Tap delete → Confirmation dialog → Story deleted
   - Tap close → Return to home
7. After last story → Auto return to home

#### Scenario 3: Viewing Others' Stories

1. User clicks on another user's story icon
2. socialhomescreen12 opens
3. API fetches all story groups (feed)
4. Stories reordered to start with clicked user
5. First story displays with:
   - Story image filling screen
   - User's profile photo and username
   - Time ago indicator
   - Progress bars
   - Close button (no delete button)
6. Story marked as viewed automatically
7. Story auto-advances after 5 seconds
8. After user's last story → Moves to next user's first story
9. User can:
   - Tap left side → Previous story (or previous user)
   - Tap right side → Next story (or next user)
   - Tap close → Return to home
10. After all stories → Auto return to home

#### Scenario 4: Stories in Home Feed

1. HomeFragment loads
2. API call fetches story groups
3. Stories row displays with:
   - First position: Current user's icon
     - If user has stories: Filled colorful ring
     - If no stories: "+" indicator
   - Other positions: Other users with stories
     - Viewed stories: Grey ring
     - Unviewed stories: Colorful gradient ring
4. Story icons clickable to view

---

## Database Schema

### Stories Table
```sql
CREATE TABLE stories (
    id bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    story_id varchar(100) NOT NULL,
    user_id varchar(50) NOT NULL,
    username varchar(50) NOT NULL,
    user_photo_base64 longtext DEFAULT NULL,
    image_base64 longtext NOT NULL,
    timestamp bigint(20) UNSIGNED NOT NULL,
    expires_at bigint(20) UNSIGNED NOT NULL,
    is_close_friends_only tinyint(1) DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (id),
    UNIQUE KEY (story_id),
    INDEX (user_id),
    INDEX (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Story Views Table
```sql
CREATE TABLE story_views (
    id bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    story_id varchar(100) NOT NULL,
    viewer_user_id varchar(50) NOT NULL,
    viewed_at timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (id),
    INDEX (story_id),
    INDEX (viewer_user_id),
    UNIQUE KEY unique_view (story_id, viewer_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Testing Instructions

### 1. Setup Testing Environment

1. Ensure XAMPP is running (Apache + MySQL)
2. Verify database tables exist:
   ```sql
   SHOW TABLES LIKE 'stories';
   SHOW TABLES LIKE 'story_views';
   ```
3. Check API base URL in `RetrofitInstance.kt`:
   ```kotlin
   private const val BASE_URL = "http://192.168.1.157/socially_api/endpoints/"
   ```
4. Update IP address if needed to match your local network

### 2. Test Story Creation

1. Login to the app
2. Go to Home screen
3. Click on your profile icon in stories row (has "+" icon)
4. Select an image from gallery
5. Click "Your Story" button
6. Verify:
   - Toast shows "Story posted!"
   - App returns to home screen
   - Your story icon now has colored ring

**Expected Database State:**
```sql
SELECT * FROM stories WHERE user_id = 'YOUR_USER_ID';
-- Should show new story with correct timestamps
```

### 3. Test Viewing Your Stories

1. Click on your story icon
2. Verify:
   - Story displays full screen
   - "Your Story" text appears
   - Progress bar shows position
   - Delete button visible
   - Story auto-advances after 5 seconds
3. Test navigation:
   - Tap left side → Previous story (if multiple)
   - Tap right side → Next story
4. Test delete:
   - Click delete button
   - Confirm deletion
   - Verify story removed

**Expected Database State:**
```sql
SELECT * FROM stories WHERE story_id = 'DELETED_STORY_ID';
-- Should return 0 rows
```

### 4. Test Viewing Others' Stories

1. Have another user create a story
2. Click on their story icon
3. Verify:
   - Story displays full screen
   - Their username appears
   - No delete button
   - Progress bar shows position
4. Test view tracking:
   ```sql
   SELECT * FROM story_views 
   WHERE story_id = 'STORY_ID' AND viewer_user_id = 'YOUR_USER_ID';
   -- Should show view record
   ```

### 5. Test Story Expiration

1. Create a test story
2. Manually update expires_at to past time:
   ```sql
   UPDATE stories 
   SET expires_at = UNIX_TIMESTAMP(NOW()) * 1000 - 1000 
   WHERE story_id = 'TEST_STORY_ID';
   ```
3. Refresh home screen
4. Verify story no longer appears
5. Optionally run cleanup:
   ```bash
   curl -X POST http://192.168.1.157/socially_api/endpoints/stories/cleanup_expired.php
   ```

### 6. Test Edge Cases

**No Stories:**
- Click on user with no stories
- Verify: "No stories available" message

**Multiple Users:**
- Have 3+ users create stories
- Click on middle user's story
- Verify: Starts with that user, then advances to others

**Network Error:**
- Turn off XAMPP
- Try to create story
- Verify: Error toast with message

**Invalid Data:**
- Try to delete someone else's story (via API directly)
- Verify: 403 Forbidden error

---

## Troubleshooting

### Story Not Appearing After Creation

**Check:**
1. Database - Is story in `stories` table?
   ```sql
   SELECT * FROM stories ORDER BY created_at DESC LIMIT 1;
   ```
2. Expiration - Is `expires_at` in future?
3. HomeFragment - Is `loadStories()` being called?
4. Network logs - Check Logcat for API response

### Story Creation Error

**Common Causes:**
1. Base64 image too large → Reduce image quality in `ImageUtils.resizeBitmap()`
2. User not found → Verify user exists in `users` table
3. Network error → Check API URL and XAMPP status
4. PHP error → Check `C:\xampp\htdocs\socially_api\logs\error.log`

### Stories Not Deleting

**Check:**
1. User ownership - Does story belong to logged-in user?
2. Foreign key constraint - `story_views` should be deleted first
3. Database logs - Check for SQL errors

### View Count Not Updating

**Check:**
1. `story_views` table - Are views being recorded?
   ```sql
   SELECT * FROM story_views WHERE story_id = 'STORY_ID';
   ```
2. Unique constraint - Multiple views by same user blocked
3. API call - Check if `markStoryViewed()` is called

---

## API Response Examples

### Create Story Success
```json
{
  "success": true,
  "data": {
    "story": {
      "story_id": "1732652400000_1234",
      "user_id": "user123",
      "username": "johndoe",
      "user_photo_base64": "base64string...",
      "image_base64": "base64string...",
      "timestamp": 1732652400000,
      "expires_at": 1732738800000,
      "viewedBy": {},
      "is_close_friends_only": false
    }
  }
}
```

### Get Feed Stories Success
```json
{
  "success": true,
  "data": {
    "story_groups": [
      {
        "userId": "user123",
        "username": "johndoe",
        "userPhotoBase64": "base64string...",
        "stories": [
          {
            "story_id": "1732652400000_1234",
            "user_id": "user123",
            "username": "johndoe",
            "user_photo_base64": "base64string...",
            "image_base64": "base64string...",
            "timestamp": 1732652400000,
            "expires_at": 1732738800000,
            "viewedBy": {
              "user456": true
            },
            "is_close_friends_only": false
          }
        ]
      }
    ]
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": "Story not found"
}
```

---

## Performance Considerations

### Image Optimization
- Images are resized to max 1080px width
- JPEG quality set to 70% for compression
- Base64 encoding adds ~33% size overhead
- Consider implementing CDN for production

### Database Optimization
- Indexes on `user_id`, `expires_at` for fast queries
- Unique constraint on `story_id` for data integrity
- Composite index on `(story_id, viewer_user_id)` in story_views

### Caching Strategy
- Stories loaded once on HomeFragment
- Local list maintained during session
- Refresh on resume to get latest stories
- Consider implementing Redis cache for production

### Auto-Cleanup
- Expired stories automatically filtered in queries
- Manual cleanup via endpoint available
- Consider cron job for regular cleanup:
  ```bash
  */15 * * * * curl -X POST http://localhost/socially_api/endpoints/stories/cleanup_expired.php
  ```

---

## Future Enhancements

1. **Story Replies:** Allow users to reply to stories via DM
2. **Story Highlights:** Save stories permanently to profile
3. **Story Reactions:** Add emoji reactions to stories
4. **Story Mentions:** Tag users in stories
5. **Story Stickers:** Add text, GIFs, location stickers
6. **Analytics:** Track story views, completion rate
7. **Close Friends:** Implement close friends list feature
8. **Video Stories:** Support video uploads
9. **Story Archive:** Auto-archive stories after 24 hours
10. **Push Notifications:** Notify when someone views your story

---

## Summary

The story feature has been successfully migrated from Firebase to PHP + MySQL backend with the following improvements:

✅ **Backend:** 6 PHP endpoints handling all story operations
✅ **Frontend:** Updated Activities and ViewModels to use API
✅ **Database:** Proper schema with indexes and constraints
✅ **Features:** Create, view, delete, auto-expire, view tracking
✅ **UX:** Instagram-style navigation with auto-progress
✅ **Performance:** Optimized queries and image handling

The implementation follows modern Android architecture (MVVM, Repository pattern) and RESTful API best practices.

---

**Last Updated:** November 26, 2025
**Version:** 1.0
**Author:** AI Assistant

