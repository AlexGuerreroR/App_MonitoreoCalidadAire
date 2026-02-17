# App Monitoreo de Calidad de Aire (Android + ESP32 + PHP + MySQL)
 
## ¿Qué es este proyecto?
Este proyecto es un sistema para registrar y visualizar mediciones relacionadas con la calidad del aire. La solución está pensada para funcionar con un dispositivo (ESP32 + sensores), pero también permite entender la estructura del sistema y revisar gran parte del trabajo sin tener el hardware.
 
## Componentes (qué hace cada parte)
- **App Android (src/mobile)**: interfaz para ver lecturas, configurar parámetros y revisar pantallas del sistema.
- **Firmware ESP32 (src/firmware)**: lógica del dispositivo que lee sensores y envía lecturas al servidor.
- **Backend PHP (src/backend)**: recibe datos (lecturas) y los guarda en la base de datos.
- **Base de datos MySQL (database)**: estructura de tablas y scripts para crear el esquema.
- **Documentación (docs)**: SRS, SDD, calidad, ciclo de vida y evidencias.
- **Pruebas (tests y docs)**: evidencia y documentación de validaciones.
 
## Flujo general (explicado simple)
1) El **dispositivo (ESP32)** obtiene mediciones de sensores.
2) Esas mediciones se envían al **backend (PHP)**.
3) El backend guarda los datos en **MySQL**.
4) La **app Android** permite configurar y visualizar la información.
 
## Cómo revisar el proyecto sin conocer el hardware
Aunque no tengas ESP32 ni sensores, puedes entender y verificar el repositorio así:
 
### 1) Revisar documentación y estructura (sin ejecutar nada)
- Abre `docs/` para ver la documentación del proyecto (SRS, SDD, Quality, Lifecycle).
- Revisa `CM_PLAN.md` y `CHANGELOG.md` para entender control de configuración y versiones.
- Mira `database/` para ver el esquema y cómo se almacenan las lecturas.
 
### 2) Revisar la app Android sin ESP32
Puedes abrir la app en Android Studio y revisar pantallas, navegación y configuración:
- Ruta: `src/mobile`
- Qué puedes validar sin hardware:
  - Que el proyecto compile.
  - Que las pantallas carguen y la navegación funcione.
  - Que los formularios y vistas se vean correctamente.
  - Que las opciones de configuración estén disponibles (aunque no se conecte a un ESP32 real).
 
### 3) Revisar backend y base de datos (sin dispositivo)
Puedes levantar el backend localmente y revisar que los endpoints y el guardado existan:
- Ruta: `src/backend`
- Base de datos: scripts en `database/`
- Qué puedes validar sin hardware:
  - Que el backend esté organizado y tenga archivos de recepción/registro.
  - Que el esquema SQL exista y sea coherente con lo que se guarda.
  - Que el backend tenga configuración separada (sin incluir claves reales).
 
## Puesta en marcha (cuando sí hay hardware)
### 1) Firmware ESP32
- Abrir el `.ino` en Arduino IDE.
- Instalar librerías necesarias.
- Compilar y cargar al ESP32.
 
### 2) Backend PHP (XAMPP)
- Copiar `src/backend/php` a `htdocs` (o ajustar rutas).
- Crear la base en MySQL usando scripts en `database/`.
- Configurar variables locales (evitar subir secretos al repo).
 
### 3) App Android
- Abrir el proyecto en Android Studio desde `src/mobile`.
- Compilar e instalar en un celular/emulador.
- Conectarse al dispositivo (cuando el ESP32 esté disponible) y hacer la configuración.
 
## Estructura del repositorio (guía rápida)
- `src/mobile`: app Android
- `src/firmware`: código ESP32 (Arduino)
- `src/backend`: backend PHP
- `database`: scripts SQL
- `docs`: documentación y evidencias
- `tests`: pruebas
- `CM_PLAN.md`: plan de gestión de configuración
- `CHANGELOG.md`: historial de versiones
 
## Evidencias
Las capturas y evidencias verificables se guardan en:
- `docs/evidencias/funcional/` (pruebas funcionales)
- `docs/evidencias/auditoria_fisica_issue1/` (auditoría física del repositorio)
- `docs/evidencias/trazabilidad_issue3/` (trazabilidad y documentación)
 
## Línea base
La versión `v1.0` representa la línea base inicial del proyecto (estructura GCS, fuentes y documentación mínima).