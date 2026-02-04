# Plan de Pruebas (TEST_PLAN)

## Objetivo
Verificar que el sistema (ESP32 + App Android + Backend PHP + MySQL local) funciona y que los cambios no rompen el flujo.

## Pruebas mínimas (Baseline v1.0)

### TP-001 Compilación e instalación App Android
- Abrir src/mobile/MonitoreoAireApp en Android Studio
- Build exitoso sin errores
- Instalar APK en un dispositivo o emulador
Resultado esperado: app abre y navega sin cierres.

### TP-002 Firmware ESP32 y modo AP
- Flashear el ESP32 con el .ino
- Confirmar que emite WiFi AP "SensorAire-ESP32"
Resultado esperado: AP visible, conexión posible con clave 12345678.

### TP-003 Provisioning desde App (HTTP /config)
- Conectar el teléfono al AP del ESP32
- Enviar configuración:
  - ssid: WiFi local
  - password: clave WiFi
  - token: token del dispositivo
  - serverUrl: URL del backend
Resultado esperado: respuesta success true y reinicio del ESP32.

### TP-004 Conexión STA y envío al backend (HTTP POST JSON)
- Verificar que el ESP32 se conecta al WiFi local
- Confirmar POST al serverUrl con JSON:
  token, co2, temperatura, humedad, pm25, calidad_aire_indice
Resultado esperado: backend responde 200 y se registra lectura.

### TP-005 Persistencia MySQL local
- Verificar en MySQL (phpMyAdmin) que se insertan lecturas
Resultado esperado: registro nuevo por cada envío.

## Evidencias recomendadas
- Captura del tag v1.0
- Captura de Release v1.0
- Capturas de ejecución: AP visible, provisioning OK, inserción en DB
