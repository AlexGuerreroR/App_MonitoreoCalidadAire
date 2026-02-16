package com.example.monitoreoaire

object ApiConfig {
    // Mientras pruebas con el emulador y XAMPP:
    const val BASE_URL = "http://192.168.0.103/WSMONITOREO/"
    // luego lo cambias por tu dominio o IP pública 192.168.0.108

    // crear dispositivo (genera token)
    const val CREATE_DEVICE_URL = BASE_URL + "crear_dispositivo.php"

    // Para la ESP32: IP REAL de tu PC o servidor, accesible en la red
    // Ejemplo: si tu PC con XAMPP tiene IP 192.168.1.50
    const val SERVER_ESP_URL = "http://192.168.0.103/WSMONITOREO/insertar_lectura.php"

    // PARA CONFIGURAR LA ESP32 EN MODO AP:
    const val ESP_AP_CONFIG_URL = "http://192.168.4.1/config"

    // ApiConfig.kt  (solo agrega esta línea dentro del object ApiConfig)
    const val ULTIMA_LECTURA_URL = BASE_URL + "ultima_lectura.php"

}
