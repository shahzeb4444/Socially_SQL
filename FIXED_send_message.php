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
require_once __DIR__ . '/../../utils/fcm_helper.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::error('Method not allowed. Use POST', 405);
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if (json_last_error() !== JSON_ERROR_NONE) {
        Response::error('Invalid JSON format', 400);
    }

    $message_id = isset($data['message_id']) ? trim($data['message_id']) : '';
    $chat_id = isset($data['chat_id']) ? trim($data['chat_id']) : '';
    $sender_id = isset($data['sender_id']) ? trim($data['sender_id']) : '';
    $sender_username = isset($data['sender_username']) ? trim($data['sender_username']) : '';
    $text = isset($data['text']) ? $data['text'] : '';
    $media_type = isset($data['media_type']) ? trim($data['media_type']) : '';
    $media_url = isset($data['media_url']) ? $data['media_url'] : '';
    $media_caption = isset($data['media_caption']) ? $data['media_caption'] : '';
    $is_vanish_mode = isset($data['is_vanish_mode']) ? (int)$data['is_vanish_mode'] : 0;

    if (empty($chat_id) || empty($sender_id) || empty($sender_username)) {
        Response::error('chat_id, sender_id, and sender_username are required', 400);
    }

    if (empty($message_id)) {
        $message_id = uniqid('msg_', true);
    }

    $timestamp = time() * 1000; // milliseconds

    $database = new Database();
    $db = $database->getConnection();

    // Insert message
    $stmt = $db->prepare("
        INSERT INTO messages (
            message_id, chat_id, sender_id, sender_username,
            text, timestamp, media_type, media_url, media_caption,
            is_vanish_mode, viewed_by, vanished_for
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '', '')
    ");

    $stmt->execute([
        $message_id, $chat_id, $sender_id, $sender_username,
        $text, $timestamp, $media_type, $media_url, $media_caption,
        $is_vanish_mode
    ]);

    error_log("=== MESSAGE SAVED: $message_id ===");
    error_log("Chat ID: $chat_id");
    error_log("Sender ID: $sender_id");

    // ============================================================
    // FIXED: Extract receiver ID from concatenated chat_id
    // Format: usr_XXXXX.XXXXXXusr_YYYYY.YYYYYY (no separator)
    // ============================================================
    $receiver_id = '';

    error_log("=== EXTRACTING RECEIVER ID (FIXED) ===");

    // Method: Remove sender_id from chat_id to get receiver_id
    // Example: "usr_AAA.AAAusr_BBB.BBB" - "usr_AAA.AAA" = "usr_BBB.BBB"

    if (!empty($sender_id) && strpos($chat_id, $sender_id) !== false) {
        // Replace sender_id with empty string
        $receiver_id = str_replace($sender_id, '', $chat_id);
        error_log("After removing sender_id: '$receiver_id'");
    }

    // Clean up any remaining underscores if they exist
    $receiver_id = trim($receiver_id, '_');

    error_log("=== EXTRACTION RESULT ===");
    error_log("Chat ID: $chat_id");
    error_log("Sender ID: $sender_id");
    error_log("Receiver ID: $receiver_id");
    error_log("Receiver ID Length: " . strlen($receiver_id));

    // Verify receiver exists in database
    if (!empty($receiver_id)) {
        $verify_stmt = $db->prepare("SELECT uid, fcm_token FROM users WHERE uid = ?");
        $verify_stmt->execute([$receiver_id]);
        $receiver_data = $verify_stmt->fetch(PDO::FETCH_ASSOC);

        if ($receiver_data) {
            error_log("✅ Receiver VERIFIED in database: $receiver_id");

            if (!empty($receiver_data['fcm_token'])) {
                error_log("✅ FCM Token EXISTS for receiver");
                $fcm_token = $receiver_data['fcm_token'];

                // Detect notification type
                $is_screenshot = (strpos($text, 'Screenshot was detected') !== false);

                if ($is_screenshot) {
                    error_log("=== SCREENSHOT NOTIFICATION ===");
                    $title = "Screenshot Alert! 📸";
                    $body = "$sender_username took a screenshot of your chat";
                    $notification_type = "screenshot";

                    $notification_data = [
                        'type' => 'screenshot',
                        'chat_id' => $chat_id,
                        'screenshot_taker_id' => $sender_id,
                        'screenshot_taker_username' => $sender_username,
                        'sender_id' => $sender_id,
                        'sender_username' => $sender_username,
                        'other_user_id' => $sender_id,
                        'other_user_name' => $sender_username,
                        'timestamp' => strval($timestamp)
                    ];
                } else {
                    error_log("=== NEW MESSAGE NOTIFICATION ===");

                    // Create message preview
                    if (!empty($media_url)) {
                        if (!empty($media_caption)) {
                            $message_preview = "📷 " . (strlen($media_caption) > 50 ? substr($media_caption, 0, 50) . '...' : $media_caption);
                        } else {
                            $media_label = $media_type === 'video' ? 'Video' : 'Photo';
                            $message_preview = "📷 $media_label";
                        }
                    } else {
                        $message_preview = strlen($text) > 50 ? substr($text, 0, 50) . '...' : $text;
                    }

                    $vanish_indicator = $is_vanish_mode ? " 👻" : "";
                    $title = "New Message from $sender_username$vanish_indicator";
                    $body = $message_preview;
                    $notification_type = "new_message";

                    $notification_data = [
                        'type' => 'new_message',
                        'chat_id' => $chat_id,
                        'sender_id' => $sender_id,
                        'sender_username' => $sender_username,
                        'other_user_id' => $sender_id,
                        'other_user_name' => $sender_username,
                        'message_id' => $message_id,
                        'is_vanish_mode' => strval($is_vanish_mode),
                        'timestamp' => strval($timestamp)
                    ];
                }

                error_log("Notification Title: $title");
                error_log("Notification Body: $body");

                // Save notification to database
                try {
                    error_log("=== SAVING NOTIFICATION TO DATABASE ===");

                    $notif_stmt = $db->prepare("
                        INSERT INTO notifications
                        (user_id, from_user_id, type, title, message, data_json, is_read)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    ");

                    $data_json = json_encode($notification_data);

                    error_log("Inserting notification for user_id: $receiver_id, from_user_id: $sender_id");

                    $notif_result = $notif_stmt->execute([
                        $receiver_id,        // user_id (must exist in users.uid)
                        $sender_id,          // from_user_id
                        $notification_type,  // type
                        $title,             // title
                        $body,              // message
                        $data_json,         // data_json
                        0                   // is_read
                    ]);

                    if ($notif_result) {
                        $notification_id = $db->lastInsertId();
                        error_log("✅ SUCCESS: Notification saved to database with ID: $notification_id");
                    } else {
                        $error_info = $notif_stmt->errorInfo();
                        error_log("❌ ERROR: Failed to save notification");
                        error_log("SQL Error: " . print_r($error_info, true));
                    }
                } catch (PDOException $db_error) {
                    error_log("❌ DATABASE EXCEPTION: " . $db_error->getMessage());
                    error_log("SQL State: " . $db_error->getCode());
                }

                // Send FCM push notification
                try {
                    error_log("=== SENDING FCM NOTIFICATION ===");

                    $fcm_result = FCMHelper::sendNotification(
                        $fcm_token,
                        $title,
                        $body,
                        $notification_data
                    );

                    if (isset($fcm_result['success']) && $fcm_result['success']) {
                        error_log("✅ SUCCESS: FCM notification sent!");
                    } else {
                        error_log("❌ ERROR: FCM notification failed: " . ($fcm_result['error'] ?? 'Unknown error'));
                    }
                } catch (Exception $fcm_error) {
                    error_log("❌ FCM EXCEPTION: " . $fcm_error->getMessage());
                }
            } else {
                error_log("⚠️ WARNING: Receiver has no FCM token");
            }
        } else {
            error_log("❌ ERROR: Receiver NOT FOUND in users table: $receiver_id");
            error_log("⚠️ Cannot send notification - receiver_id doesn't exist in database");
        }
    } else {
        error_log("❌ ERROR: Could not extract receiver_id from chat_id");
    }

    // Return success response
    $formattedMessage = [
        'message_id' => $message_id,
        'chat_id' => $chat_id,
        'sender_id' => $sender_id,
        'sender_username' => $sender_username,
        'text' => $text,
        'timestamp' => (int)$timestamp,
        'is_edited' => false,
        'is_deleted' => false,
        'deleted_at' => 0,
        'media_type' => $media_type,
        'media_url' => $media_url,
        'media_caption' => $media_caption,
        'is_vanish_mode' => (bool)$is_vanish_mode,
        'viewed_by' => '',
        'vanished_for' => ''
    ];

    Response::success([
        'message' => $formattedMessage
    ], 200);

} catch (Exception $e) {
    error_log("=== EXCEPTION IN send_message.php ===");
    error_log("Error: " . $e->getMessage());
    error_log("Trace: " . $e->getTraceAsString());
    Response::error('Failed to send message: ' . $e->getMessage(), 500);
}

