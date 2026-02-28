<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

// Solo administradores y supervisores pueden editar nombres
$user = require_auth($cn);
require_role($user, ['ADMIN', 'SUPERVISOR']);

$id_dispositivo = intval($_POST['id_dispositivo'] ?? 0);
$nuevo_nombre = $_POST['nombre_dispositivo'] ?? '';

if ($id_dispositivo <= 0 || empty($nuevo_nombre)) {
    echo json_encode(["success" => false, "message" => "Datos incompletos"]);
    exit;
}

$sql = $cn->prepare("UPDATE dispositivos SET nombre_dispositivo = ? WHERE id = ?");
$sql->bind_param("si", $nuevo_nombre, $id_dispositivo);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Nombre actualizado"]);
} else {
    echo json_encode(["success" => false, "message" => "Error al actualizar"]);
}
?>