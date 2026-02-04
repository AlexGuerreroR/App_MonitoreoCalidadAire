<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$input = json_decode(file_get_contents("php://input"), true);

$email    = trim($input['email'] ?? '');
$password = $input['password'] ?? '';

if ($email === '' || $password === '') {
    echo json_encode(["success" => false, "message" => "Faltan datos"]);
    exit;
}

$sql = $cn->prepare("SELECT id, nombre, email, password, rol FROM usuarios WHERE email = ? LIMIT 1");
$sql->bind_param("s", $email);
$sql->execute();
$res = $sql->get_result();

if (!$row = $res->fetch_assoc()) {
    echo json_encode(["success" => false, "message" => "Credenciales inválidas"]);
    exit;
}

if (!password_verify($password, $row['password'])) {
    echo json_encode(["success" => false, "message" => "Credenciales inválidas"]);
    exit;
}

// Fallback: si por algún motivo no existe ADMIN todavía, promueve al primer usuario que loguea.
$adminCount = $cn->query("SELECT COUNT(*) AS total FROM usuarios WHERE rol = 'ADMIN'");
if ($adminCount) {
    $dataAdmin = $adminCount->fetch_assoc();
    if ((int)$dataAdmin['total'] === 0) {
        $upd = $cn->prepare("UPDATE usuarios SET rol = 'ADMIN' WHERE id = ?");
        $upd->bind_param("i", $row['id']);
        $upd->execute();
        $row['rol'] = 'ADMIN';
    }
}

// Generar token de sesión
$api_token = bin2hex(random_bytes(32));
$updTok = $cn->prepare("UPDATE usuarios SET api_token = ? WHERE id = ?");
$updTok->bind_param("si", $api_token, $row['id']);
$updTok->execute();

echo json_encode([
    "success" => true,
    "message" => "Login correcto",
    "id_usuario" => (int)$row['id'],
    "nombre" => $row['nombre'],
    "rol" => $row['rol'],
    "api_token" => $api_token
]);
?>