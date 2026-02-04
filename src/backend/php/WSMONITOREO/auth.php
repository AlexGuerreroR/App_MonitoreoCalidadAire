<?php
header('Content-Type: application/json');
require_once 'conexion.php';

function get_auth_header() {
    if (isset($_SERVER['HTTP_AUTHORIZATION'])) return trim($_SERVER['HTTP_AUTHORIZATION']);
    if (isset($_SERVER['Authorization'])) return trim($_SERVER['Authorization']);
    if (function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        foreach ($headers as $k => $v) {
            if (strtolower($k) === 'authorization') return trim($v);
        }
    }
    return '';
}

function get_bearer_token() {
    $auth = get_auth_header();
    if ($auth !== '' && preg_match('/Bearer\s+(\S+)/', $auth, $m)) return $m[1];
    if (isset($_GET['api_token']) && $_GET['api_token'] !== '') return $_GET['api_token'];
    if (isset($_POST['api_token']) && $_POST['api_token'] !== '') return $_POST['api_token'];
    return '';
}

function require_auth($cn) {
    $token = get_bearer_token();
    if ($token === '') {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "No autorizado"]);
        exit;
    }

    $sql = $cn->prepare("SELECT id, nombre, email, rol FROM usuarios WHERE api_token = ? LIMIT 1");
    $sql->bind_param("s", $token);
    $sql->execute();
    $res = $sql->get_result();
    if (!$row = $res->fetch_assoc()) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Sesión inválida"]);
        exit;
    }
    return $row;
}

function require_role($user, $rolesPermitidos) {
    $rol = strtoupper($user['rol'] ?? '');
    if (!in_array($rol, $rolesPermitidos, true)) {
        http_response_code(403);
        echo json_encode(["success" => false, "message" => "Acceso denegado"]);
        exit;
    }
}

function require_device_access($cn, $user, $id_dispositivo) {
    $rol = strtoupper($user['rol'] ?? '');
    if ($rol === 'ADMIN' || $rol === 'SUPERVISOR') return;

    $id_usuario = intval($user['id']);
    $sql = $cn->prepare("SELECT id FROM dispositivos WHERE id = ? AND id_usuario = ? LIMIT 1");
    $sql->bind_param("ii", $id_dispositivo, $id_usuario);
    $sql->execute();
    $res = $sql->get_result();
    if (!$res->fetch_assoc()) {
        http_response_code(403);
        echo json_encode(["success" => false, "message" => "Acceso denegado"]);
        exit;
    }
}
?>