# SRS v1 - Requisitos del Sistema

## 1. Propósito
Definir los requisitos funcionales y no funcionales del sistema de monitoreo de calidad de aire basado en Android + ESP32 + backend PHP + MySQL local.

## 2. Alcance
El sistema permite:
- Configurar el ESP32 desde la app mediante WiFi AP y endpoint HTTP /config.
- Medir temperatura, humedad (DHT11) y gas (MQ135).
- Mostrar datos en pantalla TFT.
- Enviar lecturas al backend por HTTP (JSON).
- Almacenar lecturas en MySQL local y consultar historial desde la app (si aplica).

## 3. Requisitos Funcionales (RF)
RF-001: La app debe conectarse al AP del ESP32 (SSID SensorAire-ESP32) para configuración inicial.  
RF-002: La app debe enviar configuración al ESP32 vía HTTP POST a /config con: ssid, password, token, serverUrl.  
RF-003: El ESP32 debe almacenar la configuración en memoria persistente (Preferences) y reiniciar.  
RF-004: El ESP32 debe conectarse a la red WiFi local (STA) usando ssid/password configurados.  
RF-005: El ESP32 debe leer sensores DHT11 y MQ135 y actualizar la pantalla TFT.  
RF-006: El ESP32 debe enviar lecturas al backend mediante HTTP POST en JSON incluyendo token.  
RF-007: El backend debe validar el token (si aplica) y guardar lecturas en MySQL local.  
RF-008: Debe existir opción de factory reset del ESP32 vía endpoint /factory_reset.

## 4. Requisitos No Funcionales (RNF)
RNF-001: El sistema debe operar con base de datos local (MySQL en XAMPP) sin depender de nube.  
RNF-002: No se deben versionar credenciales reales en el repositorio.  
RNF-003: El envío de lecturas debe ser estable en intervalos configurables.  
RNF-004: El código debe mantenerse versionado con tags y releases para auditoría.

## 5. Supuestos y dependencias
- XAMPP instalado con Apache y MySQL.
- ESP32 con librerías WiFi/WebServer/HTTPClient y sensores conectados.
- Android Studio para compilar la app.
