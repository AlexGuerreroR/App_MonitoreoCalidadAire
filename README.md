# App Monitoreo de Calidad de Aire (Android + ESP32 + PHP + MySQL)

## Descripción
Sistema de monitoreo de calidad de aire con:
- App Android para configurar y visualizar lecturas.
- ESP32 con sensores DHT11 (temperatura/humedad) y MQ135 (gas), y pantalla TFT.
- Backend PHP en XAMPP.
- Base de datos MySQL local para almacenar lecturas e historial.

## Flujo general
1) El ESP32 arranca en Modo AP si no está configurado:
   - SSID: SensorAire-ESP32
   - Clave: 12345678
2) La app se conecta al WiFi del ESP32 y envía configuración por HTTP:
   - POST http://192.168.4.1/config con ssid, password, token, serverUrl
3) El ESP32 reinicia, se conecta a la red local (STA) y envía lecturas al backend:
   - POST JSON a serverUrl con token, co2, temperatura, humedad, pm25, calidad_aire_indice
4) El backend guarda lecturas en MySQL local.

## Estructura del repositorio
- src/mobile: proyecto Android
- src/firmware: código del ESP32 (Arduino)
- src/backend: backend PHP (XAMPP)
- database: scripts SQL para crear BD
- docs: documentación (SRS, SDD, calidad, ciclo)
- tests: plan de pruebas
- CM_PLAN.md: plan de gestión de configuración
- CHANGELOG.md: historial de versiones

## Puesta en marcha (resumen)
1) Firmware ESP32:
   - Abrir el .ino en Arduino IDE
   - Configurar librerías (WiFi, WebServer, DHT, Adafruit_ILI9341)
   - Compilar y cargar al ESP32

2) Backend PHP (XAMPP):
   - Copiar src/backend/php a htdocs (o ajustar rutas)
   - Crear la base en MySQL usando database/schema.sql
   - Configurar variables en config/backend.env (local)

3) App Android:
   - Abrir src/mobile/MonitoreoAireApp en Android Studio
   - Compilar e instalar
   - Conectarse al AP del ESP32 y realizar provisioning

## Línea base
La versión v1.0 representa la línea base inicial (estructura GCS + fuentes + documentación mínima).
