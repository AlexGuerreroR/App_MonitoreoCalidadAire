<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$input = json_decode(file_get_contents("php://input"), true);

$nombre    = trim($input['nombre'] ?? '');
$email     = trim($input['email'] ?? '');
$password  = $input['password'] ?? '';
$pregunta  = trim($input['pregunta'] ?? '');
$respuesta = trim($input['respuesta'] ?? '');

if ($nombre === '' || $email === '' || $password === '' || $pregunta === '' || $respuesta === '') {
    echo json_encode(["success" => false, "message" => "Faltan datos"]);
    exit;
}

// Solo permitir registro si NO existe ningún usuario (primer usuario será ADMIN)
$count = $cn->query("SELECT COUNT(*) AS total FROM usuarios");
if ($count) {
    $row = $count->fetch_assoc();
    if ((int)$row['total'] > 0) {
        echo json_encode(["success" => false, "message" => "El registro está deshabilitado. Pide al ADMIN que cree tu cuenta."]);
        exit;
    }
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
$rol = 'ADMIN';

$sql = $cn->prepare("INSERT INTO usuarios(nombre,email,password,pregunta_seguridad,respuesta_seguridad,rol) VALUES (?,?,?,?,?,?)");
$sql->bind_param("ssssss", $nombre, $email, $hash, $pregunta, $respuesta, $rol);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Usuario registrado", "id_usuario" => $sql->insert_id, "rol" => $rol]);
} else {
    echo json_encode(["success" => false, "message" => "Error al registrar"]);
}
?>