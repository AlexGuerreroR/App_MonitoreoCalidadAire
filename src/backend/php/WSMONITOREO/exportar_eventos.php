<?php
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$rol = strtoupper($user['rol'] ?? 'SUPERVISOR');

$id_usuario_req = isset($_GET['id_usuario']) ? intval($_GET['id_usuario']) : 0;
$fecha          = isset($_GET['fecha']) ? trim($_GET['fecha']) : '';

if ($rol === 'TECNICO') {
    $id_usuario_req = intval($user['id']);
}

$verTodo = ($id_usuario_req === 0 && ($rol === 'ADMIN' || $rol === 'SUPERVISOR'));

if ($fecha === '') {
    http_response_code(400);
    echo "Parámetros inválidos";
    exit;
}

header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename=eventos_' . $fecha . '.csv');

$out = fopen('php://output', 'w');
fputcsv($out, ['fecha_hora','dispositivo','ubicacion','parametro','valor','umbral','mensaje']);

if ($verTodo) {
    $sql = $cn->prepare("
        SELECT e.fecha_hora, d.nombre_dispositivo, d.ubicacion, e.parametro, e.valor, e.umbral, e.mensaje
        FROM eventos e
        INNER JOIN dispositivos d ON d.id = e.id_dispositivo
        WHERE DATE(e.fecha_hora) = ?
        ORDER BY e.fecha_hora ASC
    ");
    $sql->bind_param("s", $fecha);
} else {
    if ($id_usuario_req <= 0) {
        http_response_code(400);
        echo "id_usuario inválido";
        exit;
    }
    $sql = $cn->prepare("
        SELECT e.fecha_hora, d.nombre_dispositivo, d.ubicacion, e.parametro, e.valor, e.umbral, e.mensaje
        FROM eventos e
        INNER JOIN dispositivos d ON d.id = e.id_dispositivo
        WHERE d.id_usuario = ?
          AND DATE(e.fecha_hora) = ?
        ORDER BY e.fecha_hora ASC
    ");
    $sql->bind_param("is", $id_usuario_req, $fecha);
}

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