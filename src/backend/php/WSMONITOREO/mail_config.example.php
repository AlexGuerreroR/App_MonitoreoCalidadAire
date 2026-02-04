<?php
// Copia este archivo como mail_config.php y configura tus credenciales.
// Si no configuras esto, el sistema seguirá registrando eventos, solo que no enviará correo.

define('MAIL_ENABLED', false);
define('MAIL_HOST', 'smtp.gmail.com');
define('MAIL_PORT', 587);
define('MAIL_USER', 'TU_CORREO@gmail.com');
define('MAIL_PASS', 'TU_APP_PASSWORD');
define('MAIL_FROM', 'TU_CORREO@gmail.com');
define('MAIL_FROM_NAME', 'Monitoreo de Aire');
?>