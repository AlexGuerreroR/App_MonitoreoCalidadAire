<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$rol = strtoupper($user['rol']);
$mi_id = intval($user['id']);

// 1. Averiguar el ID del ADMIN dueño de los dispositivos
if ($rol === 'ADMIN') {
    // Si soy el Admin, soy dueño de mis dispositivos
    $id_dueño = $mi_id;
} else {
    // Si soy Técnico o Supervisor, busco a mi creador
    $query = $cn->prepare("SELECT id_admin_creador FROM usuarios WHERE id = ?");
    $query->bind_param("i", $mi_id);
    $query->execute();
    $resQuery = $query->get_result();
    $rowUsuario = $resQuery->fetch_assoc();

    if (!empty($rowUsuario['id_admin_creador'])) {
        $id_dueño = intval($rowUsuario['id_admin_creador']);
    } else {
        // FALLBACK: Si es un usuario viejo sin creador asignado, toma los dispositivos del primer ADMIN
        $queryAdmin = $cn->query("SELECT id FROM usuarios WHERE rol = 'ADMIN' ORDER BY id ASC LIMIT 1");
        $adminData = $queryAdmin->fetch_assoc();
        $id_dueño = $adminData ? intval($adminData['id']) : $mi_id;
    }
}

// 2. Traer los dispositivos de ese dueño
$sql = $cn->prepare("SELECT id, nombre_dispositivo, ubicacion, token_dispositivo FROM dispositivos WHERE id_usuario = ?");
$sql->bind_param("i", $id_dueño);
$sql->execute();

$res = $sql->get_result();
$datos = [];
while ($row = $res->fetch_assoc()) {
    $datos[] = $row;
}

echo json_encode($datos);
?>