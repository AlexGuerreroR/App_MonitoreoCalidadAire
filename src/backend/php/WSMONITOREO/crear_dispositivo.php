<?php
header('Content-Type: application/json');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
require_role($user, ['ADMIN', 'SUPERVISOR']); // si no aplica, elimina esta línea

$input = json_decode(file_get_contents("php://input"), true);
if (!is_array($input)) {
    $input = $_POST;
}

$nombre = trim($input['nombre_dispositivo'] ?? '');
$ubicacion = trim($input['ubicacion'] ?? '');

if ($nombre === '') {
    echo json_encode(["success" => false, "message" => "Datos incompletos"]);
    exit;
}

/* Usa SIEMPRE el id del usuario autenticado */
$id_usuario = intval($user['id'] ?? 0); // <- ESTE ERA EL ERROR
if ($id_usuario <= 0) {
    echo json_encode(["success" => false, "message" => "No se pudo identificar al usuario logueado"]);
    exit;
}

$token = bin2hex(random_bytes(16));

/* Umbrales por defecto (ajusta a los parámetros que realmente usas en tu tabla umbrales) */
$umbrales_default = [
    ['CO2', 800.0],
    ['TEMP', 27.0],
    ['HUM', 65.0],
    ['INDICE', 60.0],
    ['PM25', 35.0],
];

try {
    $cn->begin_transaction();

    $stmt = $cn->prepare("
        INSERT INTO dispositivos (id_usuario, nombre_dispositivo, ubicacion, token_dispositivo)
        VALUES (?,?,?,?)
    ");
    $stmt->bind_param("isss", $id_usuario, $nombre, $ubicacion, $token);

    if (!$stmt->execute()) {
        throw new Exception("Error al crear dispositivo");
    }

    $id_dispositivo = $stmt->insert_id;

    $stmt_u = $cn->prepare("
        INSERT INTO umbrales (id_dispositivo, parametro, valor_maximo)
        VALUES (?,?,?)
    ");

    foreach ($umbrales_default as $u) {
        $parametro = $u[0];
        $valor_maximo = floatval($u[1]);

        $stmt_u->bind_param("isd", $id_dispositivo, $parametro, $valor_maximo);

        if (!$stmt_u->execute()) {
            throw new Exception("Error al crear umbrales por defecto");
        }
    }

    $cn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Dispositivo creado con umbrales por defecto",
        "id_dispositivo" => $id_dispositivo,
        "token_dispositivo" => $token
    ]);
} catch (Exception $e) {
    $cn->rollback();
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>
