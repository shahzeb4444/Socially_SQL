# Offline Message Sync - Testing Checklist

## Prerequisites
- [ ] Two devices/emulators with the app installed (Device A = Sender, Device B = Receiver)
- [ ] Both logged in with different accounts
- [ ] XAMPP Apache and MySQL running
- [ ] Network connection available

## Test 1: Basic Offline Send (CRITICAL)

**Device A (Sender):**
1. [ ] Open chat with Device B user
2. [ ] Turn OFF WiFi/Mobile Data (Airplane mode)
3. [ ] Send message: "Test 1 - Offline message"
4. [ ] ✅ Verify message appears in chat immediately
5. [ ] Turn ON WiFi/Mobile Data
6. [ ] Wait 5 seconds
7. [ ] Open Logcat and search for "Triggered immediate sync"

**Device B (Receiver):**
1. [ ] Keep app open in chat screen
2. [ ] Wait 5-10 seconds after Device A turns on internet
3. [ ] ✅ Verify message "Test 1 - Offline message" appears

**Expected Result**: ✅ Message syncs and appears on receiver within 5-10 seconds

---

## Test 2: Multiple Offline Messages

**Device A (Sender):**
1. [ ] Turn OFF internet
2. [ ] Send 5 messages:
   - "Message 1"
   - "Message 2"
   - "Message 3"
   - "Message 4"
   - "Message 5"
3. [ ] ✅ All 5 appear on sender's screen
4. [ ] Turn ON internet
5. [ ] Wait 10 seconds

**Device B (Receiver):**
1. [ ] Wait 10 seconds
2. [ ] ✅ All 5 messages should appear in order

**Expected Result**: ✅ All messages sync in correct order

---

## Test 3: App Closed While Offline

**Device A (Sender):**
1. [ ] Turn OFF internet
2. [ ] Send message: "Test 3 - App restart"
3. [ ] ✅ Message appears on sender
4. [ ] **Close app completely** (swipe from recent apps)
5. [ ] Turn ON internet
6. [ ] Wait 5 seconds
7. [ ] **Reopen app**
8. [ ] Navigate back to chat

**Device B (Receiver):**
1. [ ] Wait 15 seconds after Device A reopens app
2. [ ] ✅ Message "Test 3 - App restart" should appear

**Expected Result**: ✅ Message syncs even after app restart

---

## Test 4: Image/Media While Offline

**Device A (Sender):**
1. [ ] Turn OFF internet
2. [ ] Send an image with caption "Test image offline"
3. [ ] ✅ Image preview appears on sender
4. [ ] Turn ON internet
5. [ ] Wait 10 seconds (images take longer)

**Device B (Receiver):**
1. [ ] Wait 15 seconds
2. [ ] ✅ Image with caption should appear

**Expected Result**: ✅ Image syncs (may take 10-15 seconds due to size)

---

## Test 5: Mixed Online/Offline

**Device A (Sender):**
1. [ ] Online - Send "Message 1 - Online"
2. [ ] ✅ Should appear on receiver immediately (2-3 seconds)
3. [ ] Turn OFF internet
4. [ ] Send "Message 2 - Offline"
5. [ ] Turn ON internet
6. [ ] Send "Message 3 - Online again"

**Device B (Receiver):**
1. [ ] "Message 1" appears immediately
2. [ ] Wait for "Message 2" (should appear in 5-10 seconds)
3. [ ] "Message 3" appears immediately

**Expected Result**: ✅ All messages appear in correct order

---

## Test 6: Vanish Mode Offline

**Device A (Sender):**
1. [ ] Turn OFF internet
2. [ ] Send message in **Vanish Mode**: "Vanish test"
3. [ ] ✅ Message appears with 👻 indicator
4. [ ] Turn ON internet
5. [ ] Wait 5 seconds

**Device B (Receiver):**
1. [ ] Wait 10 seconds
2. [ ] ✅ Message appears with vanish mode indicator
3. [ ] Close chat and reopen
4. [ ] ✅ Message should disappear

**Expected Result**: ✅ Vanish mode works correctly even for offline messages

---

## Logcat Verification

### Open Logcat (Android Studio → Logcat)

**Filter by tag**: `SociallyApplication|MessageRepository|SyncWorker|socialhomescreenchat`

### Expected Logs When Internet Restored:

```
✅ SociallyApplication: Network state changed: ONLINE
✅ SociallyApplication: Triggering immediate sync for pending offline data
✅ socialhomescreenchat: Network state: ONLINE
✅ socialhomescreenchat: Triggered immediate sync for pending messages
✅ SyncWorker: Found X pending items to sync
✅ SyncWorker: Syncing message: chatId=xxx, localId=local_xxx
✅ SyncWorker: Message synced successfully. Server ID: msg_xxx
✅ SyncWorker: Sync complete: X succeeded, 0 failed
```

---

## Database Verification (Optional)

### Check Room Database

1. [ ] Android Studio → View → Tool Windows → App Inspection
2. [ ] Select your app and "Database Inspector"
3. [ ] Check `messages` table:
   - [ ] Offline messages should have `syncStatus = "synced"`
   - [ ] `isSynced = 1` (true)
4. [ ] Check `sync_queue` table:
   - [ ] Should be empty after successful sync
   - [ ] Or items with `status = "failed"` if errors

---

## Common Issues During Testing

### Issue: Message not appearing on receiver
**Check**:
- [ ] Is XAMPP Apache running?
- [ ] Is MySQL running?
- [ ] Is receiver polling for messages? (should auto-poll every 2 seconds)
- [ ] Check Logcat for HTTP errors

### Issue: "Triggered immediate sync" not in logs
**Check**:
- [ ] Is NetworkMonitor detecting the network change?
- [ ] Try toggling WiFi OFF and ON again
- [ ] Check if SociallyApplication is set in AndroidManifest.xml

### Issue: Sync fails with HTTP 500
**Check**:
- [ ] Check XAMPP error logs: `C:\xampp\apache\logs\error.log`
- [ ] Check PHP error logs in: `C:\xampp\php\logs\php_error_log`
- [ ] Verify database connection in PHP

### Issue: Duplicate messages
**Solution**: 
- This should not happen (sync queue item is deleted)
- If it does, check if `updateMessageId()` is working

---

## Success Criteria

✅ **ALL tests passed** = Offline sync is working perfectly

✅ **Most tests passed** = Minor issues to debug

❌ **Most tests failed** = Check setup (XAMPP, internet, base URL)

---

## Performance Benchmarks

- **Online message delivery**: 1-3 seconds
- **Offline message sync**: 5-10 seconds after internet restore
- **Image sync**: 10-15 seconds after internet restore
- **App restart sync**: Automatic on app launch

---

## Final Verification

After all tests:

1. [ ] Messages sent offline appear on receiver ✅
2. [ ] No duplicate messages ✅
3. [ ] Correct message order ✅
4. [ ] Vanish mode works ✅
5. [ ] Images/media sync ✅
6. [ ] No crashes ✅
7. [ ] Logcat shows sync logs ✅

**If all checked**: 🎉 **OFFLINE MESSAGE SYNC IS WORKING PERFECTLY!** 🎉

