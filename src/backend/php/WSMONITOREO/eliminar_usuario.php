<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$id = $_POST['id'] ?? 0;

if ($id > 0) {
    $sql = $cn->prepare("DELETE FROM usuarios WHERE id = ?");
    $sql->bind_param("i", $id);
    $success = $sql->execute();
    echo json_encode(["success" => $success]);
} else {
    echo json_encode(["success" => false]);
}
?>