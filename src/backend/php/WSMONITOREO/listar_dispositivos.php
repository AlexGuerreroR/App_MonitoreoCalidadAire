<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$rol = strtoupper($user['rol']);

$id_usuario_req = intval($_GET['id_usuario'] ?? 0);

if ($rol === 'TECNICO') {
    $id_usuario = intval($user['id']);
} else {
    $id_usuario = $id_usuario_req > 0 ? $id_usuario_req : intval($user['id']);
}

if ($id_usuario <= 0) {
    echo json_encode([]);
    exit;
}

if ($rol === 'ADMIN' || $rol === 'SUPERVISOR') {
    // Si pide su propio id, se filtra; si envía 0, devuelve todo
    if ($id_usuario_req === 0) {
        $sql = $cn->prepare("SELECT id, nombre_dispositivo, ubicacion, token_dispositivo FROM dispositivos");
        $sql->execute();
    } else {
        $sql = $cn->prepare("SELECT id, nombre_dispositivo, ubicacion, token_dispositivo FROM dispositivos WHERE id_usuario = ?");
        $sql->bind_param("i", $id_usuario);
        $sql->execute();
    }
} else {
    $sql = $cn->prepare("SELECT id, nombre_dispositivo, ubicacion, token_dispositivo FROM dispositivos WHERE id_usuario = ?");
    $sql->bind_param("i", $id_usuario);
    $sql->execute();
}

$res = $sql->get_result();
$datos = [];
while ($row = $res->fetch_assoc()) $datos[] = $row;
echo json_encode($datos);
?>