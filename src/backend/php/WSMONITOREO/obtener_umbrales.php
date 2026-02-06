<?php
// obtener_umbrales.php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
// Permite que cualquier rol autenticado pueda leer (incluye TECNICO).
require_role($user, ['ADMIN', 'SUPERVISOR', 'TECNICO']);

$id_usuario_auth = intval($user['id'] ?? 0);
$id_usuario_post = intval($_POST['id_usuario'] ?? 0);
$id_usuario = ($id_usuario_post > 0) ? $id_usuario_post : $id_usuario_auth;

$id_dispositivo = intval($_POST['id_dispositivo'] ?? 0);

if ($id_usuario <= 0) {
    echo json_encode(["success" => false, "message" => "Usuario inválido"]);
    exit;
}

if ($id_dispositivo <= 0) {
    echo json_encode(["success" => false, "message" => "Dispositivo inválido"]);
    exit;
}

// Verifica que el dispositivo pertenezca al usuario objetivo
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

// Defaults
$data = [
    "CO2" => 800,
    "TEMP" => 27,
    "HUM" => 65,
    "INDICE" => 60
];

$sql = $cn->prepare("SELECT parametro, valor_maximo FROM umbrales WHERE id_dispositivo = ?");
if (!$sql) {
    echo json_encode(["success" => false, "message" => "Error preparando consulta"]);
    exit;
}

$sql->bind_param("i", $id_dispositivo);
$sql->execute();
$res = $sql->get_result();

while ($row = $res->fetch_assoc()) {
    $p = strtoupper(trim($row['parametro']));
    $v = floatval($row['valor_maximo']);
    if (array_key_exists($p, $data)) {
        $data[$p] = $v;
    }
}

echo json_encode(["success" => true, "data" => $data]);
