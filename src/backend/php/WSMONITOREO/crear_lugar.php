<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$input = json_decode(file_get_contents("php://input"), true);

$id_usuario = intval($input['id_usuario'] ?? 0);
$nombre     = $input['nombre_lugar'] ?? '';
$desc       = $input['descripcion_lugar'] ?? '';

if($id_usuario==0 || $nombre==''){
    echo json_encode(["success"=>false,"message"=>"Faltan datos"]);
    exit;
}

$sql = $cn->prepare("INSERT INTO lugares(id_usuario,nombre_lugar,descripcion) VALUES (?,?,?)");
$sql->bind_param("iss", $id_usuario, $nombre, $desc);

if($sql->execute()){
    echo json_encode(["success"=>true,"message"=>"Lugar creado"]);
}else{
    echo json_encode(["success"=>false,"message"=>"Error al crear lugar"]);
}
?>
