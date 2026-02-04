<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);

$id_dispositivo = intval($_GET['id_dispositivo'] ?? 0);
$limit = intval($_GET['limit'] ?? 50);
if ($limit <= 0 || $limit > 500) $limit = 50;

if ($id_dispositivo <= 0) {
    echo json_encode([]);
    exit;
}

require_device_access($cn, $user, $id_dispositivo);

$sql = $cn->prepare("
    SELECT id, co2, pm25, temperatura, humedad, calidad_aire_indice, fecha_hora
    FROM lecturas
    WHERE id_dispositivo = ?
    ORDER BY fecha_hora DESC
    LIMIT ?
");
$sql->bind_param("ii", $id_dispositivo, $limit);
$sql->execute();
$res = $sql->get_result();

$datos = [];
while ($row = $res->fetch_assoc()) $datos[] = $row;
echo json_encode($datos);
?>