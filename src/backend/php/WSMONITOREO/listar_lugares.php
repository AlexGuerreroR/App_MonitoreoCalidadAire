<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$id_usuario = $_GET['id_usuario'] ?? 0;
$id_usuario = intval($id_usuario);

$sql = $cn->prepare("SELECT id, nombre_lugar, descripcion FROM lugares WHERE id_usuario = ?");
$sql->bind_param("i", $id_usuario);
$sql->execute();
$res = $sql->get_result();

$data = [];
while($row = $res->fetch_assoc()){
    $data[] = $row;
}

echo json_encode($data);
?>
