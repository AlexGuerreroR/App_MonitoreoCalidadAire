<?php
// guardar_umbrales.php  (misma URL que ya usas, pero ahora soporta id_dispositivo)
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$id_usuario = intval($user['id'] ?? 0);

require_role($user, ['ADMIN', 'SUPERVISOR']);

if ($id_usuario <= 0) {
    echo json_encode(["success" => false, "message" => "Usuario inválido"]);
    exit;
}

$id_dispositivo = intval($_POST['id_dispositivo'] ?? 0);

$umbral_co2    = isset($_POST['umbral_co2'])    ? floatval($_POST['umbral_co2'])    : null;
$umbral_temp   = isset($_POST['umbral_temp'])   ? floatval($_POST['umbral_temp'])   : null;
$umbral_hum    = isset($_POST['umbral_hum'])    ? floatval($_POST['umbral_hum'])    : null;
$umbral_indice = isset($_POST['umbral_indice']) ? floatval($_POST['umbral_indice']) : null;

if ($umbral_co2 === null || $umbral_temp === null || $umbral_hum === null || $umbral_indice === null) {
    echo json_encode(["success" => false, "message" => "Datos incompletos"]);
    exit;
}

/*
  Compatibilidad:
  - Si envías id_dispositivo: guarda SOLO ese dispositivo (lo que quieres).
  - Si NO envías id_dispositivo: mantiene tu comportamiento anterior (todos los dispositivos del usuario).
*/
$idsDispositivos = [];

if ($id_dispositivo > 0) {
    // Verifica que el dispositivo sea del usuario logueado
    $sqlCheck = $cn->prepare("SELECT id FROM dispositivos WHERE id = ? AND id_usuario = ?");
    $sqlCheck->bind_param("ii", $id_dispositivo, $id_usuario);
    $sqlCheck->execute();
    $resCheck = $sqlCheck->get_result();

    if (!$resCheck->fetch_assoc()) {
        echo json_encode(["success" => false, "message" => "No autorizado o dispositivo no existe"]);
        exit;
    }

    $idsDispositivos[] = $id_dispositivo;
} else {
    // Comportamiento anterior: tomar todos los dispositivos del usuario
    $sqlDisp = $cn->prepare("SELECT id FROM dispositivos WHERE id_usuario = ?");
    $sqlDisp->bind_param("i", $id_usuario);
    $sqlDisp->execute();
    $resDisp = $sqlDisp->get_result();

    while ($row = $resDisp->fetch_assoc()) {
        $idsDispositivos[] = intval($row['id']);
    }

    if (empty($idsDispositivos)) {
        echo json_encode(["success" => false, "message" => "El usuario no tiene dispositivos"]);
        exit;
    }
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

$cn->begin_transaction();
try {
    foreach ($idsDispositivos as $idDisp) {
        guardarUmbral($sqlUmbral, $idDisp, 'CO2',    $umbral_co2);
        guardarUmbral($sqlUmbral, $idDisp, 'TEMP',   $umbral_temp);
        guardarUmbral($sqlUmbral, $idDisp, 'HUM',    $umbral_hum);
        guardarUmbral($sqlUmbral, $idDisp, 'INDICE', $umbral_indice);
    }

    $cn->commit();
    echo json_encode(["success" => true, "message" => "Umbrales actualizados correctamente"]);
} catch (Exception $e) {
    $cn->rollback();
    echo json_encode(["success" => false, "message" => "Error guardando umbrales"]);
}
