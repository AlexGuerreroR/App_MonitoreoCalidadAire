<?php
header('Content-Type: application/json; charset=utf-8');
require_once 'conexion.php';
require_once 'auth.php';

$user = require_auth($cn);
$rol = strtoupper($user['rol'] ?? 'SUPERVISOR');
$mi_id = intval($user['id']);

$fecha = isset($_POST['fecha']) ? trim($_POST['fecha']) : '';
if ($fecha === '') {
    $fecha = date('Y-m-d');
}

// 1. Averiguar de quién son los dispositivos (Lógica Jerárquica)
if ($rol === 'ADMIN') {
    $id_dueño = $mi_id;
} else {
    $query = $cn->prepare("SELECT id_admin_creador FROM usuarios WHERE id = ?");
    $query->bind_param("i", $mi_id);
    $query->execute();
    $resQuery = $query->get_result();
    $rowUsuario = $resQuery->fetch_assoc();

    if (!empty($rowUsuario['id_admin_creador'])) {
        $id_dueño = intval($rowUsuario['id_admin_creador']);
    } else {
        $queryAdmin = $cn->query("SELECT id FROM usuarios WHERE rol = 'ADMIN' ORDER BY id ASC LIMIT 1");
        $adminData = $queryAdmin->fetch_assoc();
        $id_dueño = $adminData ? intval($adminData['id']) : $mi_id;
    }
}

// 2. Ejecutar la consulta basada en el dueño
$sql = $cn->prepare("
    SELECT e.id, e.parametro, e.valor, e.umbral, e.mensaje, e.fecha_hora,
           d.nombre_dispositivo, d.ubicacion
    FROM eventos e
    INNER JOIN dispositivos d ON d.id = e.id_dispositivo
    WHERE d.id_usuario = ? AND DATE(e.fecha_hora) = ?
    ORDER BY e.fecha_hora DESC
");
$sql->bind_param("is", $id_dueño, $fecha);
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
    if ($parametro === 'CO2') $tituloParam = 'CO₂';
    elseif ($parametro === 'PM25') $tituloParam = 'PM2.5';

    $titulo = $tituloParam . " alto en " . $nombreDisp;
    if ($parametro === 'INDICE') $titulo = "Índice de calidad bajo en " . $nombreDisp;

    $detalle = "Valor: $valor · Umbral: $umbral";
    if (!empty($ubicacion)) $detalle .= " · Ubicación: $ubicacion";

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