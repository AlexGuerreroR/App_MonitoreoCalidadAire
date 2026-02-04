<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
require_role($user, ['ADMIN', 'SUPERVISOR']);

$id_usuario = intval($_POST['id_usuario'] ?? 0);

$umbral_co2    = isset($_POST['umbral_co2'])    ? floatval($_POST['umbral_co2'])    : null;
$umbral_temp   = isset($_POST['umbral_temp'])   ? floatval($_POST['umbral_temp'])   : null;
$umbral_hum    = isset($_POST['umbral_hum'])    ? floatval($_POST['umbral_hum'])    : null;
$umbral_indice = isset($_POST['umbral_indice']) ? floatval($_POST['umbral_indice']) : null;

if ($id_usuario <= 0 || $umbral_co2 === null || $umbral_temp === null || $umbral_hum === null || $umbral_indice === null) {
    echo json_encode(["success" => false, "message" => "Datos incompletos"]);
    exit;
}

$sqlDisp = $cn->prepare("SELECT id FROM dispositivos WHERE id_usuario = ?");
$sqlDisp->bind_param("i", $id_usuario);
$sqlDisp->execute();
$resDisp = $sqlDisp->get_result();

$idsDispositivos = [];
while ($row = $resDisp->fetch_assoc()) $idsDispositivos[] = intval($row['id']);

if (empty($idsDispositivos)) {
    echo json_encode(["success" => false, "message" => "El usuario no tiene dispositivos"]);
    exit;
}

$sqlUmbral = $cn->prepare("
    INSERT INTO umbrales (id_dispositivo, parametro, valor_maximo)
    VALUES (?, ?, ?)
    ON DUPLICATE KEY UPDATE valor_maximo = VALUES(valor_maximo)
");

if (!$sqlUmbral) {
    echo json_encode(["success" => false, "message" => "Error preparando consulta de umbrales"]);
    exit;
}

function guardarUmbral($stmt, $idDisp, $parametro, $valor) {
    $parametro = strtoupper($parametro);
    $stmt->bind_param("isd", $idDisp, $parametro, $valor);
    $stmt->execute();
}

foreach ($idsDispositivos as $idDisp) {
    guardarUmbral($sqlUmbral, $idDisp, 'CO2',    $umbral_co2);
    guardarUmbral($sqlUmbral, $idDisp, 'TEMP',   $umbral_temp);
    guardarUmbral($sqlUmbral, $idDisp, 'HUM',    $umbral_hum);
    guardarUmbral($sqlUmbral, $idDisp, 'INDICE', $umbral_indice);
}

echo json_encode(["success" => true, "message" => "Umbrales actualizados correctamente"]);
?>