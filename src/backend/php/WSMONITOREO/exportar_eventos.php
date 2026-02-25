<?php
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$rol = strtoupper($user['rol'] ?? 'SUPERVISOR');
$mi_id = intval($user['id']);

$fecha = isset($_GET['fecha']) ? trim($_GET['fecha']) : '';

if ($fecha === '') {
    http_response_code(400);
    echo "Parámetros inválidos";
    exit;
}

// 1. Lógica Jerárquica
if ($rol === 'ADMIN') {
    $id_dueño = $mi_id;
} else {
    $query = $cn->prepare("SELECT id_admin_creador FROM usuarios WHERE id = ?");
    $query->bind_param("i", $mi_id);
    $query->execute();
    $resQuery = $query->get_result();
    $rowUsuario = $resQuery->fetch_assoc();

    if (!empty($rowUsuario['id_admin_creador'])) {
        $id_dueño = intval($rowUsuario['id_admin_creador']);
    } else {
        $queryAdmin = $cn->query("SELECT id FROM usuarios WHERE rol = 'ADMIN' ORDER BY id ASC LIMIT 1");
        $adminData = $queryAdmin->fetch_assoc();
        $id_dueño = $adminData ? intval($adminData['id']) : $mi_id;
    }
}

header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename=eventos_' . $fecha . '.csv');

$out = fopen('php://output', 'w');
fputcsv($out, ['fecha_hora','dispositivo','ubicacion','parametro','valor','umbral','mensaje']);

$sql = $cn->prepare("
    SELECT e.fecha_hora, d.nombre_dispositivo, d.ubicacion, e.parametro, e.valor, e.umbral, e.mensaje
    FROM eventos e
    INNER JOIN dispositivos d ON d.id = e.id_dispositivo
    WHERE d.id_usuario = ? AND DATE(e.fecha_hora) = ?
    ORDER BY e.fecha_hora ASC
");
$sql->bind_param("is", $id_dueño, $fecha);
$sql->execute();
$res = $sql->get_result();

while ($row = $res->fetch_assoc()) {
    fputcsv($out, [
        $row['fecha_hora'],
        $row['nombre_dispositivo'],
        $row['ubicacion'],
        $row['parametro'],
        $row['valor'],
        $row['umbral'],
        $row['mensaje']
    ]);
}

fclose($out);
?>