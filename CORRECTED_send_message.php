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

    // Insert message with vanish mode fields
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

    // Log message saved
    error_log("=== MESSAGE SAVED ===");
    error_log("Message ID: $message_id");
    error_log("Chat ID: $chat_id");
    error_log("Sender: $sender_id ($sender_username)");
    error_log("Text: $text");

    // Extract receiver ID from chat_id (format: userA_userB or userB_userA)
    $receiver_id = '';

    if (strpos($chat_id, '_') !== false) {
        // Format: user1_user2
        $chat_parts = explode('_', $chat_id);
        foreach ($chat_parts as $part) {
            if (!empty($part) && $part !== $sender_id) {
                $receiver_id = $part;
                break;
            }
        }
    } else {
        // Alternative format: concatenated IDs
        $receiver_id = str_replace($sender_id, '', $chat_id);
    }

    error_log("Extracted receiver_id: '$receiver_id' from chat_id: '$chat_id'");

    // Send push notification to receiver if receiver_id found
    if (!empty($receiver_id)) {
        // Get receiver's FCM token
        $token_stmt = $db->prepare("SELECT uid, fcm_token FROM users WHERE uid = ?");
        $token_stmt->execute([$receiver_id]);
        $token_row = $token_stmt->fetch(PDO::FETCH_ASSOC);

        if ($token_row) {
            error_log("Receiver FOUND in database");
            error_log("Receiver UID: " . $token_row['uid']);
            error_log("FCM Token: " . (!empty($token_row['fcm_token']) ? 'EXISTS (length: ' . strlen($token_row['fcm_token']) . ')' : 'NULL/EMPTY'));
        } else {
            error_log("ERROR: Receiver NOT found in database for uid: '$receiver_id'");
        }

        if ($token_row && !empty($token_row['fcm_token'])) {
            $fcm_token = $token_row['fcm_token'];

            // Check if this is a screenshot message
            $is_screenshot = (strpos($text, 'Screenshot was detected') !== false);

            if ($is_screenshot) {
                error_log("=== SCREENSHOT MESSAGE DETECTED ===");

                // Screenshot notification
                $title = "Screenshot Alert! 📸";
                $body = "$sender_username took a screenshot of your chat";
                $notification_type = "screenshot";

                $notification_data = [
                    'type' => 'screenshot',
                    'chat_id' => $chat_id,
                    'screenshot_taker_id' => $sender_id,
                    'screenshot_taker_username' => $sender_username,
                    'timestamp' => strval($timestamp)
                ];
            } else {
                error_log("=== REGULAR MESSAGE ===");

                // Regular message notification
                if (!empty($media_url)) {
                    // Media message
                    $message_preview = !empty($media_caption) ? "📷 $media_caption" : "📷 Photo";
                } else {
                    // Text message - truncate if too long
                    $message_preview = mb_strlen($text) > 50 ? mb_substr($text, 0, 50) . '...' : $text;
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
                    'message_id' => $message_id,
                    'is_vanish_mode' => $is_vanish_mode ? 'true' : 'false',
                    'timestamp' => strval($timestamp)
                ];
            }

            error_log("Notification Type: $notification_type");
            error_log("Title: $title");
            error_log("Body: $body");

            // Save notification to database
            // Column order: id, user_id, from_user_id, type, title, message, data_json, is_read, created_at
            try {
                error_log("=== SAVING NOTIFICATION TO DATABASE ===");

                // Prepare data
                $notif_user_id = $receiver_id;
                $notif_from_user_id = $sender_id;
                $notif_type = $notification_type;
                $notif_title = $title;
                $notif_message = $body;
                $notif_data_json = json_encode($notification_data);
                $notif_is_read = 0;

                error_log("user_id: $notif_user_id");
                error_log("from_user_id: $notif_from_user_id");
                error_log("type: $notif_type");
                error_log("title: $notif_title");
                error_log("message: $notif_message");
                error_log("data_json: $notif_data_json");
                error_log("is_read: $notif_is_read");

                // Insert with explicit column names matching your database order
                $notif_stmt = $db->prepare("
                    INSERT INTO notifications
                    (user_id, from_user_id, type, title, message, data_json, is_read)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                ");

                $notif_result = $notif_stmt->execute([
                    $notif_user_id,
                    $notif_from_user_id,
                    $notif_type,
                    $notif_title,
                    $notif_message,
                    $notif_data_json,
                    $notif_is_read
                ]);

                if ($notif_result) {
                    $last_id = $db->lastInsertId();
                    error_log("SUCCESS: Notification saved to database with ID: $last_id");
                } else {
                    $error_info = $notif_stmt->errorInfo();
                    error_log("FAILED to save notification to database");
                    error_log("PDO Error: " . print_r($error_info, true));
                }
            } catch (Exception $db_error) {
                error_log("EXCEPTION saving notification to database: " . $db_error->getMessage());
                error_log("Stack trace: " . $db_error->getTraceAsString());
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
                    error_log("SUCCESS: FCM notification sent!");
                } else {
                    error_log("FAILED: FCM notification not sent");
                    error_log("FCM Error: " . ($fcm_result['error'] ?? 'Unknown error'));
                }
            } catch (Exception $fcm_error) {
                // Log error but continue - notification not critical for message delivery
                error_log("EXCEPTION sending FCM: " . $fcm_error->getMessage());
            }
        } else {
            if (!$token_row) {
                error_log("SKIP: Receiver not found in users table");
            } else {
                error_log("SKIP: Receiver has no FCM token");
            }
        }
    } else {
        error_log("ERROR: Could not extract receiver_id from chat_id");
    }

    // Prepare response
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

    error_log("=== RETURNING SUCCESS RESPONSE ===");

    Response::success([
        'message' => $formattedMessage
    ], 200);

} catch (Exception $e) {
    error_log("=== EXCEPTION IN send_message.php ===");
    error_log("Error: " . $e->getMessage());
    error_log("Stack trace: " . $e->getTraceAsString());
    Response::error('Failed to send message: ' . $e->getMessage(), 500);
}

