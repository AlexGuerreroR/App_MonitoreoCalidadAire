<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
require_role($user, ['ADMIN', 'SUPERVISOR']);

$id_dispositivo = intval($_POST['id_dispositivo'] ?? 0);

if ($id_dispositivo <= 0) {
    echo json_encode(["success" => false, "message" => "id_dispositivo inválido"]);
    exit;
}

$sql = $cn->prepare("DELETE FROM dispositivos WHERE id = ?");
$sql->bind_param("i", $id_dispositivo);

if ($sql->execute()) {
    echo json_encode(["success" => true, "message" => "Dispositivo eliminado"]);
} else {
    echo json_encode(["success" => false, "message" => "Error al eliminar"]);
}
?>