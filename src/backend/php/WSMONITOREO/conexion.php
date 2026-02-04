<?php
$host = "localhost";
$user = "root";
$pass = "";
$db   = "monitoreo_aire";

$cn = new mysqli($host, $user, $pass, $db);
if($cn->connect_error){
    die("Error de conexión: " . $cn->connect_error);
}
$cn->set_charset("utf8");
?>
