<?php

$host = "localhost";
$user = "root";
$pass = "";
$db   = "monitoreo_aire";

$cn = new mysqli($host, $user, $pass, $db);
if($cn->connect_error){
    die("Error de conexión: " . $cn->connect_error);
}

// Configurar el conjunto de caracteres
$cn->set_charset("utf8");


?>