<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$id_admin = $_GET['id_admin'] ?? 0;
$id_admin = intval($id_admin);

if ($id_admin <= 0) {
    echo json_encode([]);
    exit;
}

// CORRECCIÓN: Cambiado 'correo' por 'email' según tu tabla
$sql = $cn->prepare("SELECT id, nombre, email, rol FROM usuarios WHERE id_admin_creador = ? ORDER BY nombre ASC");

if (!$sql) {
    echo json_encode(["error" => $cn->error]);
    exit;
}

$sql->bind_param("i", $id_admin);
$sql->execute();
$res = $sql->get_result();

$data = [];
while($row = $res->fetch_assoc()){
    $data[] = $row;
}

echo json_encode($data);
?>