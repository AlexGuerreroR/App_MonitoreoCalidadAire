<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

// Validar token del Admin
$user = require_auth($cn);
require_role($user, ['ADMIN']);

$input = json_decode(file_get_contents("php://input"), true);

// Ajustado a las llaves que envía tu Android
$nombre    = trim($input['nombre'] ?? '');
$email     = trim($input['email'] ?? '');
$password  = $input['password'] ?? '';
$pregunta  = trim($input['pregunta_seguridad'] ?? '');
$respuesta = trim($input['respuesta_seguridad'] ?? '');
$rol       = strtoupper(trim($input['rol'] ?? 'SUPERVISOR'));
$id_admin  = intval($input['id_admin_creador'] ?? 0);

if ($nombre === '' || $email === '' || $password === '' || $pregunta === '' || $respuesta === '' || $id_admin === 0) {
    echo json_encode(["success" => false, "message" => "Faltan datos"]);
    exit;
}

// Verificar si el correo ya existe
$check = $cn->prepare("SELECT id FROM usuarios WHERE email = ? LIMIT 1");
$check->bind_param("s", $email);
$check->execute();
if ($check->get_result()->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "El correo ya está registrado"]);
    exit;
}

$hash = password_hash($password, PASSWORD_BCRYPT);
$api_token = bin2hex(random_bytes(16)); // Generar token para el nuevo usuario

// INSERT con id_admin_creador y api_token
$sql = $cn->prepare("INSERT INTO usuarios (nombre, email, password, pregunta_seguridad, respuesta_seguridad, rol, id_admin_creador, api_token) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
$sql->bind_param("ssssssis", $nombre, $email, $hash, $pregunta, $respuesta, $rol, $id_admin, $api_token);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Usuario creado correctamente"]);
} else {
    echo json_encode(["success" => false, "message" => "Error al crear usuario: " . $cn->error]);
}
?>