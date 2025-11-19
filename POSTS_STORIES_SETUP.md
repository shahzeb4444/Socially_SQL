# Firebase to PHP+MySQL Conversion - Posts & Stories Feature

## ✅ COMPLETED: Login & Registration with PHP+MySQL
## ✅ COMPLETED: Posts & Stories Feature with PHP+MySQL

---

## 📋 SETUP INSTRUCTIONS

### **STEP 1: Import Database Tables**

1. **Open phpMyAdmin**: http://localhost/phpmyadmin
2. **Select Database**: Click on `socially_db` (left sidebar)
3. **Import SQL File**:
   - Click the **Import** tab at the top
   - Click **Choose File**
   - Navigate to: `C:\xampp\htdocs\socially_api\setup_posts_stories.sql`
   - Click **Go** at the bottom
4. **Verify Tables Created**:
   - Click on `socially_db` in left sidebar
   - You should see these new tables:
     - `posts`
     - `post_likes`
     - `post_comments`
     - `stories`
     - `story_views`
     - `user_posts`
     - `follows`

### **STEP 2: Update Your IP Address**

**For Physical Device (WiFi Testing):**

1. Find your PC's IP address:
   ```cmd
   ipconfig
   ```
   Look for "IPv4 Address" under your WiFi adapter (e.g., `192.168.1.100`)

2. Update `RetrofitInstance.kt`:
   - File location: `app/src/main/java/com/teamsx/i230610_i230040/network/RetrofitInstance.kt`
   - Change line 34:
     ```kotlin
     private const val BASE_URL = "http://YOUR_IP_HERE/socially_api/endpoints/"
     ```
     Example: `"http://192.168.1.100/socially_api/endpoints/"`

**For Android Emulator:**
   - Change to: `"http://10.0.2.2/socially_api/endpoints/"`

### **STEP 3: Test Your Setup**

1. **Start XAMPP**:
   - Open XAMPP Control Panel
   - Start **Apache** (port 80)
   - Start **MySQL** (port 3307)

2. **Test Backend in Browser**:
   - Open: http://YOUR_IP/socially_api/test.php
   - Should show: `{"status":"OK","message":"Socially API is running!"}`

3. **Rebuild Android App**:
   - In Android Studio: **Build → Rebuild Project**
   - Wait for Gradle sync to complete

4. **Run App and Test**:
   - Install app on your physical device
   - **Login** with existing account
   - Go to **Home** screen
   - Click **+** button → Select **Post** or **Story**

---

## 🎯 WHAT'S NOW WORKING

### ✅ **1. Create Post (PHP Backend)**
- Select 1-3 images
- Add description and location
- Posts saved to MySQL `posts` table
- Images stored as Base64 in database

### ✅ **2. Create Story (PHP Backend)**
- Select 1 image
- Post to "Your Story" or "Close Friends"
- Stories saved to MySQL `stories` table
- Auto-expires after 24 hours

### ✅ **3. Home Feed - Posts**
- Loads posts from MySQL via API
- Shows posts from you + users you follow
- Displays username, profile pic, images, description
- Like/Unlike functionality via API
- Comments count

### ✅ **4. Home Feed - Stories**
- Horizontal scroll of story circles
- Stories from you + users you follow
- Colored ring for unviewed stories
- Click to view story (Firebase still used for viewing - will convert later)

---

## 📁 NEW FILES CREATED

### **Backend (PHP)**
```
C:\xampp\htdocs\socially_api\
├── models/
│   ├── Post.php          (NEW - Post model with CRUD operations)
│   └── Story.php         (NEW - Story model with 24hr expiry logic)
├── endpoints/
│   ├── posts/
│   │   ├── create.php    (NEW - Create post API)
│   │   ├── get_feed.php  (NEW - Get feed posts API)
│   │   └── toggle_like.php (NEW - Like/Unlike post API)
│   └── stories/
│       ├── create.php    (NEW - Create story API)
│       ├── get_feed.php  (NEW - Get feed stories API)
│       └── mark_viewed.php (NEW - Mark story as viewed API)
└── setup_posts_stories.sql (NEW - Database schema)
```

### **Android (Kotlin)**
```
app/src/main/java/com/teamsx/i230610_i230040/
├── network/
│   ├── PostRepository.kt   (NEW - Post API repository)
│   ├── StoryRepository.kt  (NEW - Story API repository)
│   └── ApiModels.kt        (UPDATED - Added Post/Story DTOs)
├── viewmodels/
│   ├── PostViewModel.kt    (NEW - Post ViewModel with LiveData)
│   └── StoryViewModel.kt   (NEW - Story ViewModel with LiveData)
├── CreatePostScreen.kt     (UPDATED - Now uses PHP API)
├── socialhomescreen15.kt   (UPDATED - Now uses PHP API)
└── HomeFragment.kt         (UPDATED - Loads posts/stories from API)
```

---

## 🔄 WHAT STILL USES FIREBASE

These features will be converted in upcoming steps:

1. ❌ **Follow System** (still Firebase)
2. ❌ **Comments** (still Firebase)
3. ❌ **Messaging** (still Firebase)
4. ❌ **Story Viewing** (still Firebase)
5. ❌ **User Profile Updates** (still Firebase)
6. ❌ **Search Users** (still Firebase)

---

## 🧪 HOW TO TEST

### **Test 1: Create a Post**
1. Login to app
2. Go to Home → Click **+** button
3. Select **Post**
4. Pick 1-3 images
5. Write description (required)
6. Add location (optional)
7. Click **Post** button
8. Should show "Post created successfully!"
9. Navigate back to Home → You should see your post

### **Test 2: Create a Story**
1. Login to app
2. Go to Home → Click **+** button
3. Select **Story**
4. Pick 1 image
5. Click **Your Story** or **Close Friends**
6. Should show "Story posted!"
7. Navigate back to Home → You should see your story circle at the top

### **Test 3: Like a Post**
1. Scroll through feed on Home screen
2. Tap the **heart icon** on any post
3. Heart should turn red (liked)
4. Tap again → Heart turns gray (unliked)

### **Test 4: Verify in Database**
1. Open phpMyAdmin: http://localhost/phpmyadmin
2. Select `socially_db` → Click `posts` table
3. Click **Browse** → You should see your posts with Base64 images
4. Click `stories` table → You should see your stories
5. Click `post_likes` table → You should see like records

---

## ⚠️ TROUBLESHOOTING

### **Error: "Network error, please check your connection"**
**Solution:**
- Verify XAMPP Apache & MySQL are running
- Test backend: http://YOUR_IP/socially_api/test.php in browser
- Check IP address in `RetrofitInstance.kt` matches your PC's IP
- Disable Windows Firewall temporarily to test
- Make sure phone and PC are on same WiFi network

### **Error: "Failed to create post"**
**Solution:**
- Check Logcat for error details
- Verify database tables exist (`posts`, `stories`, etc.)
- Test endpoint directly in browser or Postman:
  ```
  POST http://YOUR_IP/socially_api/endpoints/posts/create.php
  Content-Type: application/json
  
  {
    "post_id": "test123",
    "user_id": "YOUR_UID",
    "description": "Test post",
    "images": ["base64string"],
    "timestamp": 1700000000000
  }
  ```

### **Stories/Posts not showing in feed**
**Solution:**
- The feed shows posts/stories from users you **follow**
- To see your own content, you need to follow yourself OR
- The backend automatically includes your own posts/stories in feed
- Check database: `follows` table should have entries

### **Images not displaying**
**Solution:**
- Images are stored as Base64 in database
- Very large images may cause issues - images are resized to 1080px
- Check database field `image1_base64` has data (starts with `/9j/` for JPEG)

---

## 🎬 NEXT STEPS (Chronologically)

Now that Login + Posts + Stories are using PHP+MySQL, here's what to convert next:

### **Priority 1: Follow System**
- Convert `/users/{uid}/following` from Firebase to MySQL
- Create `follows` table CRUD operations
- API endpoints: `follow_user.php`, `unfollow_user.php`, `get_followers.php`, `get_following.php`

### **Priority 2: User Profile Management**
- Convert profile updates (bio, profile pic, cover pic)
- API endpoints: `update_profile.php`, `get_user_profile.php`

### **Priority 3: Comments System**
- Convert post comments from Firebase to MySQL
- API endpoints: `add_comment.php`, `get_comments.php`, `delete_comment.php`

### **Priority 4: Search Users**
- Convert user search from Firebase to MySQL
- API endpoint: `search_users.php`

### **Priority 5: Messaging**
- Complex conversion - requires real-time capabilities
- May keep Firebase for messages or use WebSockets

---

## 📊 DATABASE SCHEMA

### **posts table**
```sql
- id (BIGINT, AUTO_INCREMENT)
- post_id (VARCHAR 100, UNIQUE)
- user_id (VARCHAR 50)
- username (VARCHAR 50)
- user_photo_base64 (LONGTEXT)
- location (VARCHAR 255)
- description (TEXT)
- image1_base64 (LONGTEXT)
- image2_base64 (LONGTEXT)
- image3_base64 (LONGTEXT)
- timestamp (BIGINT)
- likes_count (INT)
- comments_count (INT)
- created_at (TIMESTAMP)
```

### **stories table**
```sql
- id (BIGINT, AUTO_INCREMENT)
- story_id (VARCHAR 100, UNIQUE)
- user_id (VARCHAR 50)
- username (VARCHAR 50)
- user_photo_base64 (LONGTEXT)
- image_base64 (LONGTEXT)
- timestamp (BIGINT)
- expires_at (BIGINT) -- 24 hours after timestamp
- is_close_friends_only (BOOLEAN)
- created_at (TIMESTAMP)
```

### **post_likes table**
```sql
- id (BIGINT, AUTO_INCREMENT)
- post_id (VARCHAR 100)
- user_id (VARCHAR 50)
- created_at (TIMESTAMP)
- UNIQUE(post_id, user_id)
```

---

## ✅ VERIFICATION CHECKLIST

Before considering Posts/Stories conversion complete:

- [ ] Database tables `posts`, `stories`, `post_likes`, `story_views` exist
- [ ] Can create post with 1-3 images successfully
- [ ] Can create story (regular & close friends)
- [ ] Posts appear in home feed
- [ ] Stories appear in horizontal scroll
- [ ] Can like/unlike posts
- [ ] Like count updates in real-time
- [ ] Posts show correct username, profile pic, timestamp
- [ ] Stories show colored ring for unviewed
- [ ] Backend returns proper JSON responses
- [ ] No Firebase calls for creating posts/stories
- [ ] Logcat shows Retrofit API calls (no Firebase)

---

**Status: Posts & Stories feature successfully converted from Firebase to PHP+MySQL! 🎉**

Next: Follow system conversion (when you're ready)

