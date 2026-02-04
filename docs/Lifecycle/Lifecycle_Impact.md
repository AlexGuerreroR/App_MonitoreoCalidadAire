# Impacto del control de cambios en el ciclo

Requisitos:
- Cambios en RF/RNF se registran en SRS y se reflejan en changelog.

Diseño:
- Cambios de arquitectura o endpoints se registran en SDD.

Implementación:
- Cambios en app/firmware/backend deben ir en rama change/REQ-XXX con commits claros.

Pruebas:
- Cada cambio debe ejecutar al menos las pruebas mínimas relacionadas (TEST_PLAN).

Release:
- Cuando el cambio es estable, se etiqueta (tag) y se publica release.
