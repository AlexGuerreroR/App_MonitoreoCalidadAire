<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
require_role($user, ['ADMIN']);

$input = json_decode(file_get_contents("php://input"), true);

$nombre    = trim($input['nombre'] ?? '');
$email     = trim($input['email'] ?? '');
$password  = $input['password'] ?? '';
$pregunta  = trim($input['pregunta'] ?? '');
$respuesta = trim($input['respuesta'] ?? '');
$rol       = strtoupper(trim($input['rol'] ?? 'SUPERVISOR'));

if ($nombre === '' || $email === '' || $password === '' || $pregunta === '' || $respuesta === '') {
    echo json_encode(["success" => false, "message" => "Faltan datos"]);
    exit;
}

if (!in_array($rol, ['SUPERVISOR', 'TECNICO'], true)) {
    echo json_encode(["success" => false, "message" => "Rol inválido"]);
    exit;
}

$check = $cn->prepare("SELECT id FROM usuarios WHERE email = ? LIMIT 1");
$check->bind_param("s", $email);
$check->execute();
$check->store_result();
if ($check->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "El correo ya está registrado"]);
    exit;
}

$hash = password_hash($password, PASSWORD_BCRYPT);

$sql = $cn->prepare("INSERT INTO usuarios(nombre,email,password,pregunta_seguridad,respuesta_seguridad,rol) VALUES (?,?,?,?,?,?)");
$sql->bind_param("ssssss", $nombre, $email, $hash, $pregunta, $respuesta, $rol);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Usuario creado", "id_usuario" => $sql->insert_id, "rol" => $rol]);
} else {
    echo json_encode(["success" => false, "message" => "Error al crear usuario"]);
}
?>