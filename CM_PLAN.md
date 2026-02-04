# Plan de Gestión de Configuración (CM Plan)

## Objetivo
Mantener el proyecto controlado, trazable y auditable, protegiendo el estado funcional del sistema y registrando cambios de forma ordenada.

## Línea base (Baseline)
Se define como línea base la versión v1.0, que incluye:
- Estructura del repositorio y reglas de versionado.
- Código fuente (Android, ESP32, backend PHP).
- Documentación mínima (SRS, SDD, calidad, ciclo).
- Scripts SQL de la base local (schema/seed).
- Plan de pruebas.

La línea base permite volver a un estado estable y reproducible.

## Control de cambios
- Rama principal: main (solo versiones estables).
- Cambios se realizan en ramas: change/REQ-XXX
- Cada cambio debe incluir:
  - descripción en commit
  - actualización de CHANGELOG si aplica
  - evidencia de prueba (según TEST_PLAN)

## Versionado
- Tags: v1.0, v1.1, v1.2...
- Releases en GitHub asociados a cada tag estable.

## Elementos de Configuración (EC)
| ID | Elemento | Ubicación | Tipo | Razón de control |
|---|---|---|---|---|
| EC-001 | App Android | src/mobile/MonitoreoAireApp | Código | Núcleo de la aplicación |
| EC-002 | Firmware ESP32 | src/firmware/arduino | Código | Lectura sensores, AP, envío HTTP |
| EC-003 | Backend PHP | src/backend/php | Código | Persistencia y validación de datos |
| EC-004 | Esquema BD | database/schema.sql | Datos | Reproducir BD local sin copiar datos reales |
| EC-005 | Datos semilla | database/seed.sql | Datos | Pruebas controladas (opcional) |
| EC-006 | Config ejemplo | config/config.example | Config | Documenta parámetros sin secretos |
| EC-007 | Env ejemplo backend | config/backend.env.example | Config | Conexión MySQL local sin credenciales reales |
| EC-008 | Requisitos (SRS) | docs/SRS/SRS_v1.md | Documento | Alcance y requisitos |
| EC-009 | Diseño (SDD) | docs/SDD/SDD_v1.md | Documento | Arquitectura y módulos |
| EC-010 | Modelo de calidad | docs/Quality/Quality_Model.md | Documento | Criterios de calidad |
| EC-011 | Impacto en ciclo | docs/Lifecycle/Lifecycle_Impact.md | Documento | Control de cambios por fases |
| EC-012 | Plan de pruebas | tests/TEST_PLAN.md | Documento | Validación del sistema |
| EC-013 | Registro de cambios | CHANGELOG.md | Documento | Historial de versiones |
| EC-014 | README | README.md | Documento | Guía de uso y estructura |

## Reglas de seguridad
- No subir credenciales reales: config/config.local, config/backend.env
- No subir local.properties ni builds (controlados por .gitignore)
