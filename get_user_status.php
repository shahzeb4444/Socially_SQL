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

    $user_id = isset($data['user_id']) ? trim($data['user_id']) : '';

    if (empty($user_id)) {
        Response::error('user_id is required', 400);
    }

    $database = new Database();
    $db = $database->getConnection();

    // Get user's online status
    $stmt = $db->prepare("
        SELECT uid, is_online, last_seen
        FROM users
        WHERE uid = ?
    ");

    $stmt->execute([$user_id]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($user) {
        // Consider user offline if last_seen is more than 15 seconds ago
        // This handles cases where app crashed and onDestroy wasn't called
        $current_time = time() * 1000;
        $time_diff = $current_time - (int)$user['last_seen'];
        $is_actually_online = ((int)$user['is_online'] === 1) && ($time_diff < 15000);

        // Auto-update database if user appears offline but is_online flag is still 1
        if (!$is_actually_online && (int)$user['is_online'] === 1) {
            $update_stmt = $db->prepare("UPDATE users SET is_online = 0 WHERE uid = ?");
            $update_stmt->execute([$user_id]);
        }

        Response::success([
            'user_id' => $user['uid'],
            'is_online' => $is_actually_online,
            'last_seen' => (int)$user['last_seen']
        ], 200);
    } else {
        Response::error('User not found', 404);
    }

} catch (Exception $e) {
    Response::error('Failed to get status: ' . $e->getMessage(), 500);
}

