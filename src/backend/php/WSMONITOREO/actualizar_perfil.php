<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);

$id_usuario = intval($user['id']);
$nombre = trim($_POST['nombre'] ?? '');
$email  = trim($_POST['email'] ?? '');

if ($nombre === '' || $email === '') {
    echo json_encode(["success" => false, "message" => "Datos incompletos"]);
    exit;
}

$sql = $cn->prepare("UPDATE usuarios SET nombre = ?, email = ? WHERE id = ?");
$sql->bind_param("ssi", $nombre, $email, $id_usuario);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Perfil actualizado"]);
} else {
    echo json_encode(["success" => false, "message" => "Error al actualizar"]);
}
?>