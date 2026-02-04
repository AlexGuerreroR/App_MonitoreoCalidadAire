# SDD v1 - Diseño del Sistema

## 1. Arquitectura general
Componentes:
- App Android (cliente): provisioning y visualización.
- ESP32 (dispositivo): sensores, AP/STA, endpoints, envío JSON.
- Backend PHP: endpoint receptor de lecturas, validación y persistencia.
- MySQL local: almacenamiento de lecturas y dispositivos.

## 2. Módulos principales

### 2.1 ESP32
- Modo AP: crea SSID/clave, levanta servidor HTTP en puerto 80
  - GET / (informativo)
  - POST /config (recibe ssid, password, token, serverUrl)
  - GET /factory_reset
- Modo STA: conecta a WiFi y envía lecturas al backend
- Sensores:
  - DHT11: temperatura y humedad
  - MQ135: lectura analógica
- Pantalla TFT:
  - visualización de estado y valores

### 2.2 Backend PHP
- Endpoint de telemetría:
  - recibe JSON con token y mediciones
  - inserta en MySQL
- Configuración por variables (archivo local no versionado)

### 2.3 App Android
- Pantalla de conexión al AP y configuración
- Pantalla de monitoreo (si aplica)
- Historial (si aplica)

## 3. Datos (modelo lógico mínimo)
- devices(token, alias, created_at)
- readings(device_token, co2, temperatura, humedad, pm25, calidad_indice, created_at)
