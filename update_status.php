<?php
}
    Response::error('Failed to update status: ' . $e->getMessage(), 500);
} catch (Exception $e) {

    }
        Response::error('Failed to update status', 500);
    } else {
        ], 200);
            'last_seen' => $last_seen
            'is_online' => $is_online,
            'user_id' => $user_id,
        Response::success([
    if ($result) {

    ]);
        $user_id
        $last_seen,
        $is_online ? 1 : 0,
    $result = $stmt->execute([

    ");
        WHERE uid = ?
        SET is_online = ?, last_seen = ?
        UPDATE users
    $stmt = $db->prepare("

    $last_seen = time() * 1000; // milliseconds
    // Update user's online status and last_seen timestamp

    $db = $database->getConnection();
    $database = new Database();

    }
        Response::error('user_id is required', 400);
    if (empty($user_id)) {

    $is_online = isset($data['is_online']) ? (bool)$data['is_online'] : false;
    $user_id = isset($data['user_id']) ? trim($data['user_id']) : '';

    }
        Response::error('Invalid JSON format', 400);
    if (json_last_error() !== JSON_ERROR_NONE) {

    $data = json_decode($input, true);
    $input = file_get_contents('php://input');
try {

}
    Response::error('Method not allowed. Use POST', 405);
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {

require_once __DIR__ . '/../../utils/response.php';
require_once __DIR__ . '/../../config/database.php';

}
    exit;
    http_response_code(200);
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {

header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

