# Modelo de Calidad (v1)

Atributos priorizados:
1) Fiabilidad: lecturas y envío no deben fallar en operación normal.
2) Usabilidad: la configuración inicial debe ser simple (AP + formulario).
3) Mantenibilidad: cambios controlados por ramas/tags y documentación mínima.
4) Seguridad: no subir credenciales, uso de token para validar dispositivo.
5) Portabilidad: backend local en XAMPP y app en Android estándar.
6) Eficiencia: intervalos de lectura/envío controlados.

Métricas mínimas:
- Tasa de envíos exitosos (% de POST 200 OK).
- Tiempo promedio de provisioning (desde conexión AP hasta reinicio y STA OK).
