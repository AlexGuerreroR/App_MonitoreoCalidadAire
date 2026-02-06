<?php
// guardar_umbrales.php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
require_role($user, ['ADMIN', 'SUPERVISOR']);

// Usuario objetivo (compatibilidad):
// - Por defecto: usuario logueado.
// - Si viene id_usuario y el rol lo permite: usa ese id_usuario.
$id_usuario_auth = intval($user['id'] ?? 0);
$id_usuario_post = intval($_POST['id_usuario'] ?? 0);
$id_usuario = ($id_usuario_post > 0) ? $id_usuario_post : $id_usuario_auth;

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

// Construye lista de dispositivos a actualizar:
// - Si viene id_dispositivo: solo ese (y valida que pertenezca al usuario objetivo).
// - Si no: todos los dispositivos del usuario objetivo (comportamiento anterior).
$idsDispositivos = [];

if ($id_dispositivo > 0) {
    $sqlCheck = $cn->prepare("SELECT id FROM dispositivos WHERE id = ? AND id_usuario = ?");
    if (!$sqlCheck) {
        echo json_encode(["success" => false, "message" => "Error preparando validación de dispositivo"]);
        exit;
    }

    $sqlCheck->bind_param("ii", $id_dispositivo, $id_usuario);
    $sqlCheck->execute();
    $resCheck = $sqlCheck->get_result();

    if (!$resCheck->fetch_assoc()) {
        echo json_encode(["success" => false, "message" => "No autorizado o dispositivo no existe"]);
        exit;
    }

    $idsDispositivos[] = $id_dispositivo;
} else {
    $sqlDisp = $cn->prepare("SELECT id FROM dispositivos WHERE id_usuario = ?");
    if (!$sqlDisp) {
        echo json_encode(["success" => false, "message" => "Error preparando consulta de dispositivos"]);
        exit;
    }

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
    $p = strtoupper($parametro);
    $v = floatval($valor);

    $stmt->bind_param("isd", $idDisp, $p, $v);
    $ok = $stmt->execute();
    if (!$ok) {
        throw new Exception("Error ejecutando guardado de umbral");
    }
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
