<?php
header('Content-Type: application/json');
require_once 'conexion.php';

// No requiere auth.php porque es un registro público para nuevos dueños de empresa
$input = json_decode(file_get_contents("php://input"), true);

$nombre    = trim($input['nombre'] ?? '');
$email     = trim($input['email'] ?? '');
$password  = $input['password'] ?? '';
$pregunta  = trim($input['pregunta_seguridad'] ?? '');
$respuesta = trim($input['respuesta_seguridad'] ?? '');

if ($nombre === '' || $email === '' || $password === '' || $pregunta === '' || $respuesta === '') {
    echo json_encode(["success" => false, "message" => "Todos los campos son obligatorios para crear su empresa."]);
    exit;
}

// VALIDACIÓN MULTI-EMPRESA:
// Revisamos si este email ya existe como ADMIN
$check = $cn->prepare("SELECT id FROM usuarios WHERE email = ? AND rol = 'ADMIN' LIMIT 1");
$check->bind_param("s", $email);
$check->execute();
if ($check->get_result()->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Ya existe una empresa registrada con este correo."]);
    exit;
}

$hash = password_hash($password, PASSWORD_BCRYPT);
$api_token = bin2hex(random_bytes(16));

// Al ser el primer Admin de su propia empresa, su id_admin_creador es 0
$sql = $cn->prepare("INSERT INTO usuarios (nombre, email, password, pregunta_seguridad, respuesta_seguridad, rol, api_token, id_admin_creador) VALUES (?, ?, ?, ?, ?, 'ADMIN', ?, 0)");
$sql->bind_param("ssssss", $nombre, $email, $hash, $pregunta, $respuesta, $api_token);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Empresa registrada con éxito. Inicie sesión para gestionar sus técnicos."]);
} else {
    echo json_encode(["success" => false, "message" => "Error al registrar: " . $cn->error]);
}

$cn->close();
?>