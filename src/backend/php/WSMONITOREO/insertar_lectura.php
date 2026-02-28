<?php
header('Content-Type: application/json');
require_once 'conexion.php';

// 1. Cargar configuración de correo
$mailConfigPath = __DIR__ . '/mail_config.php';
if (file_exists($mailConfigPath)) {
    require_once $mailConfigPath;
}

if (!defined('MAIL_ENABLED')) {
    define('MAIL_ENABLED', false);
}

// Intervalos
define('INTERVALO_HISTORICO', 60);
define('INTERVALO_EVENTO_MINUTOS', 5);

// 2. Importación única de PHPMailer
if (MAIL_ENABLED) {
    require_once __DIR__ . '/src/PHPMailer.php';
    require_once __DIR__ . '/src/SMTP.php';
    require_once __DIR__ . '/src/Exception.php';
}

// Leer JSON de la ESP32
$input = json_decode(file_get_contents("php://input"), true);
$token  = $input['token'] ?? '';
$co2    = $input['co2'] ?? null;
$temp   = $input['temperatura'] ?? null;
$hum    = $input['humedad'] ?? null;
$pm25   = $input['pm25'] ?? null;
$indice = $input['calidad_aire_indice'] ?? null;

$ip_dispositivo = $_SERVER['REMOTE_ADDR'] ?? '';

if ($token == '') {
    echo json_encode(["success" => false, "message" => "Falta token"]);
    exit;
}

// Buscar dispositivo
$sql = $cn->prepare("SELECT id FROM dispositivos WHERE token_dispositivo = ?");
$sql->bind_param("s", $token);
$sql->execute();
$res = $sql->get_result();

if (!$row = $res->fetch_assoc()) {
    echo json_encode(["success" => false, "message" => "Token no válido"]);
    exit;
}

$id_dispositivo = $row['id'];

// --- PROCESAMIENTO DE LECTURAS ---
$sqlActual = $cn->prepare("
    INSERT INTO lecturas_actuales
    (id_dispositivo, co2, pm25, temperatura, humedad, calidad_aire_indice, fecha_hora)
    VALUES (?,?,?,?,?,?,NOW())
    ON DUPLICATE KEY UPDATE
        co2 = VALUES(co2), pm25 = VALUES(pm25), temperatura = VALUES(temperatura),
        humedad = VALUES(humedad), calidad_aire_indice = VALUES(calidad_aire_indice), fecha_hora = VALUES(fecha_hora)
");
$sqlActual->bind_param("idddds", $id_dispositivo, $co2, $pm25, $temp, $hum, $indice);
$sqlActual->execute();

// Lógica de histórico (cada 60s)
$stmtLast = $cn->prepare("SELECT fecha_hora FROM lecturas WHERE id_dispositivo = ? ORDER BY fecha_hora DESC LIMIT 1");
$stmtLast->bind_param("i", $id_dispositivo);
$stmtLast->execute();
$resLast = $stmtLast->get_result();
$guardarHistorico = (!$rowL = $resLast->fetch_assoc()) || (time() - strtotime($rowL['fecha_hora']) >= INTERVALO_HISTORICO);

if ($guardarHistorico) {
    $sql2 = $cn->prepare("INSERT INTO lecturas (id_dispositivo, co2, pm25, temperatura, humedad, calidad_aire_indice, fecha_hora) VALUES (?,?,?,?,?,?,NOW())");
    $sql2->bind_param("idddds", $id_dispositivo, $co2, $pm25, $temp, $hum, $indice);
    $sql2->execute();
}

// Actualizar IP
if ($ip_dispositivo !== '') {
    $cn->query("UPDATE dispositivos SET ip_actual = '$ip_dispositivo' WHERE id = $id_dispositivo");
}

// 3. Ejecutar verificación de alarmas
verificarAlarmas($cn, $id_dispositivo, $co2, $pm25, $temp, $hum, $indice);

echo json_encode(["success" => true, "message" => "Lectura registrada"]);

// ==========================================================
//                      FUNCIONES
// ==========================================================

function verificarAlarmas($cn, $id_dispositivo, $co2, $pm25, $temp, $hum, $indice) {
    $sql = $cn->prepare("
      SELECT u.parametro, u.valor_maximo, d.nombre_dispositivo, 
           us.nombre, us.email 
    FROM umbrales u
    INNER JOIN dispositivos d ON d.id = u.id_dispositivo
    INNER JOIN usuarios us ON (
        us.id = d.id_usuario OR          
        us.id_admin_creador = d.id_usuario 
    )
    WHERE u.id_dispositivo = ?
");
    $sql->bind_param("i", $id_dispositivo);
    $sql->execute();
    $res = $sql->get_result();

    while ($row = $res->fetch_assoc()) {
        $parametro = strtoupper($row['parametro']);
        $umbral = $row['valor_maximo'];
        $valor = match($parametro) {
            'CO2' => $co2, 'PM25' => $pm25, 'TEMP' => $temp, 'HUM' => $hum, 'INDICE' => $indice, default => null
        };

        if ($valor !== null && $umbral !== null) {
            $disparar = ($parametro === 'INDICE') ? ($valor < $umbral) : ($valor > $umbral);

            if ($disparar) {
                $sqlTrack = $cn->prepare("SELECT inicio_exceso FROM tracking_alarmas WHERE id_dispositivo = ? AND parametro = ?");
                $sqlTrack->bind_param("is", $id_dispositivo, $parametro);
                $sqlTrack->execute();
                if ($rowT = $sqlTrack->get_result()->fetch_assoc()) {
                    if ((time() - strtotime($rowT['inicio_exceso'])) >= 60) {
                        $msg = "Alarma en {$row['nombre_dispositivo']}. $parametro: $valor (Umbral: $umbral)";
                        if (registrarEventoAlarma($cn, $id_dispositivo, $parametro, $valor, $umbral, $msg)) {
                            // AQUÍ SE ENVÍA AL CORREO DE LA CONSULTA SQL
                            enviarCorreoAlarma($row['email'], $row['nombre'], $row['nombre_dispositivo'], $parametro, $valor, $umbral);
                        }
                    }
                } else {
                    $sqlIns = $cn->prepare("INSERT INTO tracking_alarmas (id_dispositivo, parametro, inicio_exceso) VALUES (?, ?, NOW())");
                    $sqlIns->bind_param("is", $id_dispositivo, $parametro);
                    $sqlIns->execute();
                }
            } else {
                $cn->query("DELETE FROM tracking_alarmas WHERE id_dispositivo = $id_dispositivo AND parametro = '$parametro'");
            }
        }
    }
}

function registrarEventoAlarma($cn, $id_dispositivo, $parametro, $valor, $umbral, $mensaje) {
    $sqlLast = $cn->prepare("SELECT fecha_hora FROM eventos WHERE id_dispositivo = ? AND parametro = ? ORDER BY fecha_hora DESC LIMIT 1");
    $sqlLast->bind_param("is", $id_dispositivo, $parametro);
    $sqlLast->execute();
    if ($rowL = $sqlLast->get_result()->fetch_assoc()) {
        if ((time() - strtotime($rowL['fecha_hora'])) / 60 < INTERVALO_EVENTO_MINUTOS) return false;
    }
    $sql = $cn->prepare("INSERT INTO eventos (id_dispositivo, parametro, valor, umbral, mensaje, fecha_hora) VALUES (?,?,?,?,?,NOW())");
    $sql->bind_param("isdds", $id_dispositivo, $parametro, $valor, $umbral, $mensaje);
    return $sql->execute();
}

function enviarCorreoAlarma($para, $nombre, $dispositivo, $parametro, $valor, $umbral) {
    if (empty($para) || !MAIL_ENABLED) return;

    $mail = new \PHPMailer\PHPMailer\PHPMailer(true);
    try {
        // Configuración usando constantes de mail_config.php
        $mail->isSMTP();
        $mail->Host       = MAIL_HOST;
        $mail->SMTPAuth   = true;
        $mail->Username   = MAIL_USER;
        $mail->Password   = MAIL_PASS;
        $mail->SMTPSecure = \PHPMailer\PHPMailer\PHPMailer::ENCRYPTION_STARTTLS;
        $mail->Port       = MAIL_PORT;
        $mail->CharSet    = 'UTF-8';

        $mail->setFrom(MAIL_FROM, MAIL_FROM_NAME);
        $mail->addAddress($para, $nombre);

        $mail->isHTML(true);
        $mail->Subject = "Alarma de $parametro en dispositivo $dispositivo";

        $dt = new DateTime("now", new DateTimeZone('America/Guayaquil'));
        $fecha = $dt->format('d-m-Y H:i:s');

        $mail->Body = "<h2>Alerta Crítica</h2><p>Hola $nombre,</p><p>Dispositivo: <strong>$dispositivo</strong></p>
                       <ul><li>Parámetro: $parametro</li><li>Valor: $valor</li><li>Umbral: $umbral</li><li>Fecha: $fecha</li></ul>";

        $mail->send();
    } catch (Exception $e) { }
}