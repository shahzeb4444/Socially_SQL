# 🚀 PHP + MySQL Authentication Setup Guide

## ✅ What Has Been Implemented

### Android App Files Created/Updated:

1. **Network Layer** (`network/` package):
   - `ApiModels.kt` - Data models for API requests/responses
   - `ApiService.kt` - Retrofit API interface
   - `RetrofitInstance.kt` - Retrofit singleton configuration
   - `Resource.kt` - Wrapper class for API responses

2. **Utils** (`utils/` package):
   - `UserPreferences.kt` - SharedPreferences helper for session management

3. **Repository & ViewModel**:
   - `AuthRepository.kt` - Updated with real API calls (removed mock data)
   - `AuthViewModel.kt` - Updated with LiveData for UI observation

4. **Activities** (Updated to use PHP backend):
   - `mainlogin.kt` - Login screen (Firebase removed, PHP integrated)
   - `second_page.kt` - Registration screen (Firebase removed, PHP integrated)
   - `login_splash.kt` - Profile splash (uses SharedPreferences)

---

## 📋 PHP Backend Files You Need to Create

### Directory Structure in XAMPP:
```
C:\xampp\htdocs\socially_api\
├── config\
│   └── Database.php
├── models\
│   └── User.php
└── endpoints\
    └── auth\
        ├── register.php
        └── login.php
```

### 1️⃣ Create `Database.php`

**Path:** `C:\xampp\htdocs\socially_api\config\Database.php`

```php
<?php
class Database {
    private $host = "localhost";
    private $db_name = "socially_db";
    private $username = "root";
    private $password = "";
    public $conn;

    public function getConnection() {
        $this->conn = null;
        try {
            $this->conn = new PDO(
                "mysql:host=" . $this->host . ";dbname=" . $this->db_name,
                $this->username,
                $this->password
            );
            $this->conn->exec("set names utf8");
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        } catch(PDOException $exception) {
            echo "Connection error: " . $exception->getMessage();
        }
        return $this->conn;
    }
}
?>
```

### 2️⃣ Create `User.php` Model

**Path:** `C:\xampp\htdocs\socially_api\models\User.php`

```php
<?php
class User {
    private $conn;
    private $table_name = "users";

    public $id;
    public $uid;
    public $email;
    public $password_hash;
    public $username;
    public $full_name;
    public $bio;
    public $profile_image_url;
    public $cover_image_url;
    public $created_at;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function emailExists() {
        $query = "SELECT id, uid, email, password_hash, username, full_name, bio, 
                  profile_image_url, cover_image_url, created_at 
                  FROM " . $this->table_name . " 
                  WHERE email = :email LIMIT 1";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":email", $this->email);
        $stmt->execute();
        
        if($stmt->rowCount() > 0) {
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $this->id = $row['id'];
            $this->uid = $row['uid'];
            $this->password_hash = $row['password_hash'];
            $this->username = $row['username'];
            $this->full_name = $row['full_name'];
            $this->bio = $row['bio'];
            $this->profile_image_url = $row['profile_image_url'];
            $this->cover_image_url = $row['cover_image_url'];
            $this->created_at = $row['created_at'];
            return true;
        }
        return false;
    }

    public function usernameExists() {
        $query = "SELECT id FROM " . $this->table_name . " WHERE username = :username LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":username", $this->username);
        $stmt->execute();
        return $stmt->rowCount() > 0;
    }

    public function create() {
        $query = "INSERT INTO " . $this->table_name . " 
                  (uid, email, password_hash, username, full_name, created_at) 
                  VALUES (:uid, :email, :password_hash, :username, :full_name, NOW())";
        
        $stmt = $this->conn->prepare($query);
        
        $this->uid = 'usr_' . uniqid() . '_' . rand(1000, 9999);
        $this->password_hash = password_hash($this->password_hash, PASSWORD_BCRYPT);
        
        $stmt->bindParam(":uid", $this->uid);
        $stmt->bindParam(":email", $this->email);
        $stmt->bindParam(":password_hash", $this->password_hash);
        $stmt->bindParam(":username", $this->username);
        $stmt->bindParam(":full_name", $this->full_name);
        
        if($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            $this->created_at = date('Y-m-d H:i:s');
            return true;
        }
        return false;
    }

    public function toArray() {
        return array(
            "id" => $this->id,
            "uid" => $this->uid,
            "email" => $this->email,
            "username" => $this->username,
            "full_name" => $this->full_name,
            "bio" => $this->bio,
            "profile_image_url" => $this->profile_image_url,
            "cover_image_url" => $this->cover_image_url,
            "created_at" => $this->created_at
        );
    }
}
?>
```

### 3️⃣ Create `register.php` Endpoint

**Path:** `C:\xampp\htdocs\socially_api\endpoints\auth\register.php`

```php
<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once '../../config/Database.php';
include_once '../../models/User.php';

$database = new Database();
$db = $database->getConnection();
$user = new User($db);

$data = json_decode(file_get_contents("php://input"));

if(
    !empty($data->email) &&
    !empty($data->password) &&
    !empty($data->username) &&
    !empty($data->full_name)
) {
    
    // Validate email format
    if (!filter_var($data->email, FILTER_VALIDATE_EMAIL)) {
        http_response_code(400);
        echo json_encode(array(
            "success" => false,
            "error" => "Invalid email format"
        ));
        exit();
    }

    // Validate password length
    if (strlen($data->password) < 6) {
        http_response_code(400);
        echo json_encode(array(
            "success" => false,
            "error" => "Password must be at least 6 characters"
        ));
        exit();
    }

    // Check if email already exists
    $user->email = $data->email;
    if($user->emailExists()) {
        http_response_code(400);
        echo json_encode(array(
            "success" => false,
            "error" => "Email already exists"
        ));
        exit();
    }

    // Check if username already taken
    $user->username = $data->username;
    if($user->usernameExists()) {
        http_response_code(400);
        echo json_encode(array(
            "success" => false,
            "error" => "Username already taken"
        ));
        exit();
    }

    // Create user
    $user->password_hash = $data->password;
    $user->full_name = $data->full_name;

    if($user->create()) {
        http_response_code(201);
        echo json_encode(array(
            "success" => true,
            "data" => array(
                "user" => $user->toArray()
            )
        ));
    } else {
        http_response_code(500);
        echo json_encode(array(
            "success" => false,
            "error" => "Unable to register user"
        ));
    }
} else {
    http_response_code(400);
    echo json_encode(array(
        "success" => false,
        "error" => "All fields are required"
    ));
}
?>
```

### 4️⃣ Create `login.php` Endpoint

**Path:** `C:\xampp\htdocs\socially_api\endpoints\auth\login.php`

```php
<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once '../../config/Database.php';
include_once '../../models/User.php';

$database = new Database();
$db = $database->getConnection();
$user = new User($db);

$data = json_decode(file_get_contents("php://input"));

if(!empty($data->email) && !empty($data->password)) {
    
    $user->email = $data->email;
    
    if($user->emailExists()) {
        if(password_verify($data->password, $user->password_hash)) {
            
            // Update last login
            $query = "UPDATE users SET last_login = NOW() WHERE id = :id";
            $stmt = $db->prepare($query);
            $stmt->bindParam(":id", $user->id);
            $stmt->execute();
            
            http_response_code(200);
            echo json_encode(array(
                "success" => true,
                "data" => array(
                    "user" => $user->toArray()
                )
            ));
        } else {
            http_response_code(401);
            echo json_encode(array(
                "success" => false,
                "error" => "Invalid password"
            ));
        }
    } else {
        http_response_code(404);
        echo json_encode(array(
            "success" => false,
            "error" => "User not found"
        ));
    }
} else {
    http_response_code(400);
    echo json_encode(array(
        "success" => false,
        "error" => "Email and password are required"
    ));
}
?>
```

---

## 🔧 Setup Instructions

### Step 1: Start XAMPP
1. Open XAMPP Control Panel
2. Start **Apache** (for PHP)
3. Start **MySQL** (for database)

### Step 2: Create Database
1. Open phpMyAdmin: `http://localhost/phpmyadmin`
2. Create database named `socially_db`
3. Run your SQL schema (users table creation)

### Step 3: Create PHP Files
1. Create all the PHP files above in the specified paths
2. Make sure directory structure is correct

### Step 4: Update Android App IP Address

**Edit:** `RetrofitInstance.kt`

```kotlin
// For Android Emulator:
private const val BASE_URL = "http://10.0.2.2/socially_api/endpoints/"

// For Physical Device on same WiFi:
// Find your PC's IP: Open CMD → type: ipconfig
// Look for "IPv4 Address" (e.g., 192.168.1.100)
private const val BASE_URL = "http://192.168.1.100/socially_api/endpoints/"
```

### Step 5: Test API Endpoints

Use **Postman** or browser to test:

#### Test Registration:
```
POST: http://localhost/socially_api/endpoints/auth/register.php

Body (JSON):
{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser",
    "full_name": "Test User"
}
```

#### Test Login:
```
POST: http://localhost/socially_api/endpoints/auth/login.php

Body (JSON):
{
    "email": "test@example.com",
    "password": "password123"
}
```

---

## 🎯 How It Works

### Login Flow:
1. User enters email & password in `mainlogin.kt`
2. `AuthViewModel.login()` is called
3. `AuthRepository.login()` makes HTTP POST to `login.php`
4. PHP validates credentials against MySQL database
5. On success, user data is saved to `SharedPreferences`
6. User is redirected to `login_splash` screen

### Registration Flow:
1. User fills form in `second_page.kt`
2. `AuthViewModel.register()` is called
3. `AuthRepository.register()` makes HTTP POST to `register.php`
4. PHP validates data and creates user in MySQL
5. On success, user data is saved to `SharedPreferences`
6. User is redirected to `login_splash` screen

### Session Management:
- User data is stored in `SharedPreferences` (NOT passwords!)
- `UserPreferences.isLoggedIn()` checks if user is logged in
- `UserPreferences.clearUser()` logs out user
- No more Firebase Auth dependency!

---

## 🐛 Troubleshooting

### "Network error" in app:
- Check XAMPP Apache is running
- Verify IP address in `RetrofitInstance.kt`
- Check firewall settings

### "Connection refused":
- For emulator: Use `10.0.2.2` instead of `localhost`
- For device: Use PC's local IP (192.168.x.x)

### "Email already exists":
- Check database if user was created
- Try different email

### PHP errors:
- Check Apache error logs in XAMPP
- Verify file paths are correct
- Check PHP syntax

---

## ✅ Testing Checklist

- [ ] XAMPP Apache & MySQL running
- [ ] Database `socially_db` created
- [ ] All PHP files created in correct paths
- [ ] Can access login.php in browser
- [ ] IP address updated in RetrofitInstance.kt
- [ ] App builds without errors
- [ ] Registration creates user in database
- [ ] Login works with created user
- [ ] User data saved in SharedPreferences
- [ ] Logout clears SharedPreferences

---

**Your app is now using PHP + MySQL instead of Firebase! 🎉**

