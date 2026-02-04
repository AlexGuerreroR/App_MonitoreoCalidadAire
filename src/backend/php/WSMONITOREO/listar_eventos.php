<?php
header('Content-Type: application/json; charset=utf-8');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$rol = strtoupper($user['rol'] ?? 'SUPERVISOR');

$id_usuario_req = isset($_POST['id_usuario']) ? intval($_POST['id_usuario']) : 0;
$fecha          = isset($_POST['fecha']) ? trim($_POST['fecha']) : '';

if ($rol === 'TECNICO') {
    $id_usuario_req = intval($user['id']);
}

$verTodo = ($id_usuario_req === 0 && ($rol === 'ADMIN' || $rol === 'SUPERVISOR'));

if ($fecha === '') {
    $fecha = date('Y-m-d');
}

if ($verTodo) {
    $sql = $cn->prepare("
        SELECT e.id, e.parametro, e.valor, e.umbral, e.mensaje, e.fecha_hora,
               d.nombre_dispositivo, d.ubicacion
        FROM eventos e
        INNER JOIN dispositivos d ON d.id = e.id_dispositivo
        WHERE DATE(e.fecha_hora) = ?
        ORDER BY e.fecha_hora DESC
    ");
    $sql->bind_param("s", $fecha);
} else {
    if ($id_usuario_req <= 0) {
        echo json_encode(['success' => false, 'message' => 'id_usuario inválido']);
        exit;
    }
    $sql = $cn->prepare("
        SELECT e.id, e.parametro, e.valor, e.umbral, e.mensaje, e.fecha_hora,
               d.nombre_dispositivo, d.ubicacion
        FROM eventos e
        INNER JOIN dispositivos d ON d.id = e.id_dispositivo
        WHERE d.id_usuario = ?
          AND DATE(e.fecha_hora) = ?
        ORDER BY e.fecha_hora DESC
    ");
    $sql->bind_param("is", $id_usuario_req, $fecha);
}

$sql->execute();
$res = $sql->get_result();

$eventos = [];
while ($row = $res->fetch_assoc()) {
    $parametro   = strtoupper($row['parametro']);
    $nombreDisp  = $row['nombre_dispositivo'];
    $ubicacion   = $row['ubicacion'];
    $valor       = (float)$row['valor'];
    $umbral      = (float)$row['umbral'];
    $fechaHora   = $row['fecha_hora'];

    $tituloParam = $parametro;
    if ($parametro === 'CO2') {
        $tituloParam = 'CO₂';
    } elseif ($parametro === 'PM25') {
        $tituloParam = 'PM2.5';
    }

    $titulo = $tituloParam . " alto en " . $nombreDisp;
    if ($parametro === 'INDICE') {
        $titulo = "Índice de calidad bajo en " . $nombreDisp;
    }

    $detalle = "Valor: $valor · Umbral: $umbral";
    if (!empty($ubicacion)) {
        $detalle .= " · Ubicación: $ubicacion";
    }

    $eventos[] = [
        'id'          => (int)$row['id'],
        'titulo'      => $titulo,
        'detalle'     => $detalle,
        'fecha_hora'  => $fechaHora,
        'parametro'   => $parametro,
        'valor'       => $valor,
        'umbral'      => $umbral,
        'dispositivo' => $nombreDisp,
        'ubicacion'   => $ubicacion
    ];
}

echo json_encode(['success' => true, 'eventos' => $eventos]);
?>