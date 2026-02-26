<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
require_role($user, ['ADMIN']);

$id     = intval($_POST['id'] ?? 0);
$nombre = trim($_POST['nombre'] ?? '');
$email  = trim($_POST['email'] ?? '');
$rol    = strtoupper(trim($_POST['rol'] ?? ''));

if ($id <= 0 || $nombre === '' || $email === '' || $rol === '') {
    echo json_encode(["success" => false, "message" => "Todos los campos son obligatorios"]);
    exit;
}

// VALIDACIÓN ANTI-DUPLICADOS:
// Buscamos si existe el email en un ID que NO sea el que estamos editando
$check = $cn->prepare("SELECT id FROM usuarios WHERE email = ? AND id != ? LIMIT 1");
$check->bind_param("si", $email, $id);
$check->execute();
if ($check->get_result()->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Error: El correo '$email' ya pertenece a otro usuario"]);
    exit;
}

// Actualización de los 3 campos
$sql = $cn->prepare("UPDATE usuarios SET nombre = ?, email = ?, rol = ? WHERE id = ?");
$sql->bind_param("sssi", $nombre, $email, $rol, $id);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Usuario actualizado"]);
} else {
    echo json_encode(["success" => false, "message" => "Error al actualizar: " . $cn->error]);
}

$sql->close();
$cn->close();
?>