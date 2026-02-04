<?php
header('Content-Type: application/json');
require_once 'conexion.php';

$input = json_decode(file_get_contents("php://input"), true);

$email        = $input['email'] ?? '';
$respuestaSeg = $input['respuesta'] ?? '';
$nuevaPass    = $input['nueva_password'] ?? '';

if($email=='' || $respuestaSeg=='' || $nuevaPass==''){
    echo json_encode(["success"=>false,"message"=>"Faltan datos"]);
    exit;
}

$sql = $cn->prepare("SELECT id, respuesta_seguridad FROM usuarios WHERE email = ?");
$sql->bind_param("s", $email);
$sql->execute();
$res = $sql->get_result();

if($row = $res->fetch_assoc()){
    if($respuestaSeg === $row['respuesta_seguridad']){
        $hash = password_hash($nuevaPass, PASSWORD_BCRYPT);
        $upd = $cn->prepare("UPDATE usuarios SET password = ? WHERE id = ?");
        $upd->bind_param("si", $hash, $row['id']);
        if($upd->execute()){
            echo json_encode(["success"=>true,"message"=>"Contraseña actualizada"]);
        }else{
            echo json_encode(["success"=>false,"message"=>"Error al actualizar"]);
        }
    }else{
        echo json_encode(["success"=>false,"message"=>"Respuesta de seguridad incorrecta"]);
    }
}else{
    echo json_encode(["success"=>false,"message"=>"Usuario no encontrado"]);
}
?>
