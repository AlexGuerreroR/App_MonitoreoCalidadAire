<?php
header('Content-Type: application/json');
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);

$id_dispositivo = intval($_GET['id_dispositivo'] ?? 0);
if ($id_dispositivo <= 0) {
    echo json_encode(["success" => false, "message" => "id_dispositivo inválido"]);
    exit;
}

require_device_access($cn, $user, $id_dispositivo);

$sql = $cn->prepare("
    SELECT 
        la.co2,
        la.pm25,
        la.temperatura,
        la.humedad,
        la.calidad_aire_indice,
        la.fecha_hora,
        d.ip_actual
    FROM lecturas_actuales la
    INNER JOIN dispositivos d ON d.id = la.id_dispositivo
    WHERE la.id_dispositivo = ?
    ORDER BY la.fecha_hora DESC
    LIMIT 1
");
$sql->bind_param("i", $id_dispositivo);
$sql->execute();
$res = $sql->get_result();

if (!$row = $res->fetch_assoc()) {
    echo json_encode(["success" => false, "message" => "Sin lecturas para este dispositivo"]);
    exit;
}

$data = [
    'co2'                 => (float)$row['co2'],
    'pm25'                => (float)$row['pm25'],
    'temperatura'         => (float)$row['temperatura'],
    'humedad'             => (float)$row['humedad'],
    'calidad_aire_indice' => (float)$row['calidad_aire_indice'],
    'fecha_hora'          => $row['fecha_hora'],
    'ip_dispositivo'      => $row['ip_actual']
];

echo json_encode(['success' => true, 'data' => $data]);
?>