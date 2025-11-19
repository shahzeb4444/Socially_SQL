# ✅ UPDATED: FCM V1 API Setup Guide

## 🎉 What Changed

Firebase deprecated the **Legacy Cloud Messaging API** and now uses **Cloud Messaging API V1**. 

**Key Differences:**
- ❌ **Old**: Used Server Key from Cloud Messaging tab
- ✅ **New**: Uses Service Account JSON file for OAuth 2.0

**Your Details:**
- **Project ID**: `i-assignment-3-socially`
- **Sender ID**: `167380756781`

---

## 📋 UPDATED Setup Steps

### ✅ Step 1: Download Firebase Service Account Key

1. Open browser and go to: https://console.firebase.google.com/
2. Select your project: **23I-0610-23I-0040_Assignment3_Socially**
3. Click **⚙️ (Settings icon)** in top left
4. Click **"Project Settings"**
5. Go to **"Service Accounts"** tab (top menu)
6. You should see: **"Firebase Admin SDK"** section
7. Click **"Generate New Private Key"** button
8. A popup will appear saying "Generate new private key?"
9. Click **"Generate Key"**
10. A JSON file will download automatically

**The file will be named something like:**
```
i-assignment-3-socially-firebase-adminsdk-abcde-1234567890.json
```

---

### ✅ Step 2: Place the Service Account File

1. **Locate the downloaded file** in your Downloads folder
2. **Rename it** to: `firebase-service-account.json`
3. **Copy the file**
4. **Paste it into**: `C:\xampp\htdocs\socially_api\config\`

**Final location should be:**
```
C:\xampp\htdocs\socially_api\config\firebase-service-account.json
```

---

### ✅ Step 3: Verify the File Contents

Open the file in a text editor and verify it contains these fields:

```json
{
  "type": "service_account",
  "project_id": "i-assignment-3-socially",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@i-assignment-3-socially.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

**Important fields to check:**
- ✅ `project_id` = "i-assignment-3-socially"
- ✅ `private_key` = starts with "-----BEGIN PRIVATE KEY-----"
- ✅ `client_email` = contains "firebase-adminsdk"

---

### ✅ Step 4: How the New System Works

**When sending a notification:**

1. **PHP reads** the service account file (`firebase-service-account.json`)
2. **PHP creates** a JWT (JSON Web Token) signed with the private key
3. **PHP exchanges** the JWT for an OAuth 2.0 access token
4. **PHP uses** the access token to call FCM V1 API
5. **FCM sends** the push notification to the device

**No more server key needed!** Everything is handled via OAuth 2.0.

---

### ✅ Step 5: Test the Setup

#### Test 1: Verify File Exists
Run this in Command Prompt:
```cmd
dir "C:\xampp\htdocs\socially_api\config\firebase-service-account.json"
```

**Should see:**
```
firebase-service-account.json
```

#### Test 2: Send a Follow Request
1. Login with User A
2. Search for User B
3. Send follow request
4. **Check User B's device** (can be closed) → Should receive notification! 🎉

#### Test 3: Check PHP Logs
If notification doesn't arrive, check:
```
C:\xampp\apache\logs\error.log
```

**Look for these messages:**
- ✅ Good: "Notification sent successfully"
- ❌ Bad: "Service account file not found"
- ❌ Bad: "Failed to get access token"

---

## 🔍 Troubleshooting

### Error: "Service account file not found"
**Solution:**
1. Verify file exists at: `C:\xampp\htdocs\socially_api\config\firebase-service-account.json`
2. Check file name is EXACTLY: `firebase-service-account.json` (no typos)
3. Make sure it's in the `config` folder, not somewhere else

### Error: "Failed to get access token"
**Solution:**
1. Open the JSON file and verify it's valid JSON
2. Check `private_key` field contains the full key with newlines (`\n`)
3. Verify `client_email` and `project_id` are correct
4. Make sure OpenSSL is enabled in PHP (check `php.ini`)

### Error: "Failed to parse service account JSON"
**Solution:**
1. The JSON file might be corrupted
2. Download it again from Firebase Console
3. Don't edit the file manually

### Notification still not received
**Solution:**
1. Check device has internet connection
2. Verify FCM token is in database (not null)
3. Check Logcat for FCM errors on Android side
4. Make sure app has notification permissions

---

## 🔐 Security Best Practices

⚠️ **IMPORTANT**: The `firebase-service-account.json` file contains **SENSITIVE CREDENTIALS**!

**DO:**
- ✅ Keep it on your server only
- ✅ Add to `.gitignore` if using Git
- ✅ Set proper file permissions (read-only for web server)

**DON'T:**
- ❌ Commit to GitHub/GitLab
- ❌ Share publicly
- ❌ Email or send to others
- ❌ Include in client-side code

---

## 📝 Updated PHP Code

The `fcm_helper.php` has been updated to:
1. ✅ Use OAuth 2.0 instead of Server Key
2. ✅ Call FCM V1 API endpoint
3. ✅ Generate JWT tokens for authentication
4. ✅ Handle access token caching (1 hour expiry)

**No changes needed on Android side!** The FCM token and notification handling remain the same.

---

## ✨ Summary

**What you need to do:**
1. ✅ Download service account JSON from Firebase Console
2. ✅ Rename to `firebase-service-account.json`
3. ✅ Place in `C:\xampp\htdocs\socially_api\config\`
4. ✅ Test by sending a follow request

**What happens automatically:**
- PHP generates OAuth 2.0 tokens
- FCM V1 API sends notifications
- Devices receive push notifications

**Your app is now using the modern, supported FCM API!** 🚀

