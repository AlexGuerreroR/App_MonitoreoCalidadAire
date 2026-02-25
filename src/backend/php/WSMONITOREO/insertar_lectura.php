<?php
header('Content-Type: application/json');
require_once 'conexion.php';

// Configuración de correo (opcional)
$mailConfigPath = __DIR__ . '/mail_config.php';
if (file_exists($mailConfigPath)) {
    require_once $mailConfigPath;
}
if (!defined('MAIL_ENABLED')) {
    define('MAIL_ENABLED', false);
}


// Intervalo mínimo entre lecturas históricas (en segundos)
define('INTERVALO_HISTORICO', 60);

// Intervalo mínimo entre eventos de alarma (en minutos)
define('INTERVALO_EVENTO_MINUTOS', 5);

// PHPMailer (solo si correo está habilitado)
if (defined('MAIL_ENABLED') && MAIL_ENABLED) {
    require_once __DIR__ . '/src/PHPMailer.php';
    require_once __DIR__ . '/src/SMTP.php';
    require_once __DIR__ . '/src/Exception.php';
}


// Leer JSON enviado por la ESP32
$input = json_decode(file_get_contents("php://input"), true);

$token  = $input['token'] ?? '';
$co2    = $input['co2'] ?? null;
$temp   = $input['temperatura'] ?? null;
$hum    = $input['humedad'] ?? null;
$pm25   = $input['pm25'] ?? null;
$indice = $input['calidad_aire_indice'] ?? null;

// IP del dispositivo (ESP32)
$ip_dispositivo = $_SERVER['REMOTE_ADDR'] ?? '';

if ($token == '') {
    echo json_encode(["success" => false, "message" => "Falta token"]);
    exit;
}

// Buscar el dispositivo por token
$sql = $cn->prepare("SELECT id FROM dispositivos WHERE token_dispositivo = ?");
$sql->bind_param("s", $token);
$sql->execute();
$res = $sql->get_result();

if (!$row = $res->fetch_assoc()) {
    echo json_encode(["success" => false, "message" => "Token no válido"]);
    exit;
}

$id_dispositivo = $row['id'];

// ================== 1) Actualizar lecturas_actuales ==================
$sqlActual = $cn->prepare("
    INSERT INTO lecturas_actuales
    (id_dispositivo, co2, pm25, temperatura, humedad, calidad_aire_indice, fecha_hora)
    VALUES (?,?,?,?,?,?,NOW())
    ON DUPLICATE KEY UPDATE
        co2 = VALUES(co2),
        pm25 = VALUES(pm25),
        temperatura = VALUES(temperatura),
        humedad = VALUES(humedad),
        calidad_aire_indice = VALUES(calidad_aire_indice),
        fecha_hora = VALUES(fecha_hora)
");

$sqlActual->bind_param(
    "idddds",
    $id_dispositivo,
    $co2,
    $pm25,
    $temp,
    $hum,
    $indice
);

if (!$sqlActual->execute()) {
    echo json_encode(["success" => false, "message" => "Error al actualizar lectura actual"]);
    exit;
}

// ================== 2) Insertar en lecturas solo cada cierto tiempo ==================
$guardarHistorico = false;

$stmtLast = $cn->prepare("SELECT fecha_hora FROM lecturas WHERE id_dispositivo = ? ORDER BY fecha_hora DESC LIMIT 1");
$stmtLast->bind_param("i", $id_dispositivo);
$stmtLast->execute();
$resLast = $stmtLast->get_result();

if ($rowLast = $resLast->fetch_assoc()) {
    $ultima = strtotime($rowLast['fecha_hora']);
    if (time() - $ultima >= INTERVALO_HISTORICO) {
        $guardarHistorico = true;
    }
} else {
    // primera vez
    $guardarHistorico = true;
}

if ($guardarHistorico) {
    $sql2 = $cn->prepare("INSERT INTO lecturas
        (id_dispositivo, co2, pm25, temperatura, humedad, calidad_aire_indice, fecha_hora)
        VALUES (?,?,?,?,?,?,NOW())");

    $sql2->bind_param(
        "idddds",
        $id_dispositivo,
        $co2,
        $pm25,
        $temp,
        $hum,
        $indice
    );
    $sql2->execute();
}

// ================== 3) Actualizar IP actual ==================
if ($ip_dispositivo !== '') {
    $sql3 = $cn->prepare("UPDATE dispositivos SET ip_actual = ? WHERE id = ?");
    $sql3->bind_param("si", $ip_dispositivo, $id_dispositivo);
    $sql3->execute();
}

// ================== 4) Verificar umbrales y enviar alarma/correo ==================
verificarAlarmas($cn, $id_dispositivo, $co2, $pm25, $temp, $hum, $indice);

// ================== 5) Respuesta a la ESP/app ==================
echo json_encode(["success" => true, "message" => "Lectura registrada"]);
exit;


// ==========================================================
//                     FUNCIONES
// ==========================================================

function verificarAlarmas($cn, $id_dispositivo, $co2, $pm25, $temp, $hum, $indice)
{
    $sql = $cn->prepare("
        SELECT u.parametro, u.valor_maximo,
               d.nombre_dispositivo,
               us.nombre AS nombre_usuario,
               us.email  AS email_usuario
        FROM umbrales u
        INNER JOIN dispositivos d ON d.id = u.id_dispositivo
        INNER JOIN usuarios us ON us.id = d.id_usuario
        WHERE u.id_dispositivo = ?
    ");
    $sql->bind_param("i", $id_dispositivo);
    $sql->execute();
    $res = $sql->get_result();

    while ($row = $res->fetch_assoc()) {
        $parametro     = strtoupper($row['parametro']); 
        $umbral        = $row['valor_maximo'];
        $nombreDisp    = $row['nombre_dispositivo'];
        $nombreUsuario = $row['nombre_usuario'];
        $emailUsuario  = $row['email_usuario'];

        $valor = null;
        switch ($parametro) {
            case 'CO2':    $valor = $co2;    break;
            case 'PM25':   $valor = $pm25;   break;
            case 'TEMP':   $valor = $temp;   break;
            case 'HUM':    $valor = $hum;    break;
            case 'INDICE': $valor = $indice; break;
        }

        if ($valor !== null && $umbral !== null) {

            $disparar = false;

            if ($parametro === 'INDICE') {
                if ($valor < $umbral) $disparar = true;
            } else {
                if ($valor > $umbral) $disparar = true;
            }

            if ($disparar) {
                // 1. Verificamos si ya tenemos un registro de que este parámetro superó el umbral
                $sqlTrack = $cn->prepare("SELECT inicio_exceso FROM tracking_alarmas WHERE id_dispositivo = ? AND parametro = ?");
                $sqlTrack->bind_param("is", $id_dispositivo, $parametro);
                $sqlTrack->execute();
                $resTrack = $sqlTrack->get_result();

                if ($rowTrack = $resTrack->fetch_assoc()) {
                    // 2. Ya estaba superando el umbral. ¿Pasaron 60 segundos?
                    $inicio = strtotime($rowTrack['inicio_exceso']);
                    if ((time() - $inicio) >= 60) {
                        
                        $mensaje = "Alarma en $nombreDisp. Parámetro: $parametro, Valor: $valor, Umbral: $umbral";

                        // El registro del evento ya tiene su propia validación de INTERVALO_EVENTO_MINUTOS
                        $insertado = registrarEventoAlarma($cn, $id_dispositivo, $parametro, $valor, $umbral, $mensaje);
                        if ($insertado) {
                            enviarCorreoAlarma($emailUsuario, $nombreUsuario, $nombreDisp, $parametro, $valor, $umbral);
                        }
                    }
                } else {
                    // 3. Es la primera vez (o el primer pico) que supera el umbral. Empezamos a contar.
                    $sqlIns = $cn->prepare("INSERT INTO tracking_alarmas (id_dispositivo, parametro, inicio_exceso) VALUES (?, ?, NOW())");
                    $sqlIns->bind_param("is", $id_dispositivo, $parametro);
                    $sqlIns->execute();
                }
            } else {
                // 4. Si el valor es normal, eliminamos cualquier conteo previo (resetea el falso positivo)
                $sqlDel = $cn->prepare("DELETE FROM tracking_alarmas WHERE id_dispositivo = ? AND parametro = ?");
                $sqlDel->bind_param("is", $id_dispositivo, $parametro);
                $sqlDel->execute();
            }
        }
    }
}
function registrarEventoAlarma($cn, $id_dispositivo, $parametro, $valor, $umbral, $mensaje)
{
    // 1) Ver si hubo un evento reciente para ese dispositivo + parámetro
    $sqlLast = $cn->prepare("
        SELECT fecha_hora
        FROM eventos
        WHERE id_dispositivo = ? AND parametro = ?
        ORDER BY fecha_hora DESC
        LIMIT 1
    ");
    $sqlLast->bind_param("is", $id_dispositivo, $parametro);
    $sqlLast->execute();
    $resLast = $sqlLast->get_result();

    if ($rowLast = $resLast->fetch_assoc()) {
        $ultima  = strtotime($rowLast['fecha_hora']);
        $minDiff = (time() - $ultima) / 60.0;

        // Si el último evento fue hace menos de X minutos, no repetimos
        if ($minDiff < INTERVALO_EVENTO_MINUTOS) {
            return false;
        }
    }

    // 2) Insertar nuevo evento
    $sql = $cn->prepare("
        INSERT INTO eventos (id_dispositivo, parametro, valor, umbral, mensaje, fecha_hora)
        VALUES (?,?,?,?,?,NOW())
    ");
    $sql->bind_param("isdds", $id_dispositivo, $parametro, $valor, $umbral, $mensaje);

    return $sql->execute();
}

function enviarCorreoAlarma($para, $nombre, $dispositivo, $parametro, $valor, $umbral)
{
    if (empty($para)) {
        return;
    }

    // Incluir PHPMailer (carpeta src en el mismo proyecto)
    require_once __DIR__ . '/src/Exception.php';
    require_once __DIR__ . '/src/PHPMailer.php';
    require_once __DIR__ . '/src/SMTP.php';

    $mail = new \PHPMailer\PHPMailer\PHPMailer(true);

    try {
        // Configuración SMTP para Gmail
        $mail->isSMTP();
        $mail->Host       = 'smtp.gmail.com';
        $mail->SMTPAuth   = true;
        $mail->Username   = 'TU_CORREO@gmail.com';   // tu correo Gmail
        $mail->Password   = 'TU_APP_PASSWORD';     // contraseña de aplicación REAL
        $mail->SMTPSecure = \PHPMailer\PHPMailer\PHPMailer::ENCRYPTION_STARTTLS;
        $mail->Port       = 587;
        $mail->CharSet    = 'UTF-8';

        // Remitente y destinatario (el remitente debe ser el mismo Gmail normalmente)
        $mail->setFrom('TU_CORREO@gmail.com', 'Sistema Monitoreo');
        $mail->addAddress($para, $nombre);

        // Contenido
        $mail->isHTML(true);
        $mail->Subject = "Alarma de $parametro en el dispositivo $dispositivo";

        // Creamos un objeto de fecha forzando la zona horaria local solo para este texto
        $dt = new DateTime("now", new DateTimeZone('America/Guayaquil'));
        $fechaLocal = $dt->format('d-m-Y H:i:s');

        $body  = "<h2>Alerta de calidad de aire</h2>";
        $body .= "<p>Hola $nombre,</p>";
        $body .= "<p>Se ha detectado una alarma en tu dispositivo <strong>$dispositivo</strong>:</p>";
        $body .= "<ul>";
        $body .= "<li>Par&aacute;metro: <strong>$parametro</strong></li>";
        $body .= "<li>Valor medido: <strong>$valor</strong></li>";
        $body .= "<li>Umbral configurado: <strong>$umbral</strong></li>";
        $body .= "<li>Fecha y hora: " . $fechaLocal . "</li>";
        $body .= "</ul>";
        $body .= "<p>Por favor revisa la zona monitoreada.</p>";

        $mail->Body = $body;

        $mail->send();
    } catch (\PHPMailer\PHPMailer\Exception $e) {
        // Puedes registrar el error si lo necesitas
        // error_log('Error al enviar correo: ' . $mail->ErrorInfo);
    }
}
