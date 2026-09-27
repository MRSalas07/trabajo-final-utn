# Listado de Módulos Funcionales

El desarrollo del MVP de **Motorsport Analytics** se estructurará mediante los siguientes módulos funcionales, ordenados según su prioridad de implementación.

### 1. Módulo de Gestión de Usuarios (Prioridad: Alta)
*   **Descripción:** Se encarga de la seguridad y personalización de la experiencia. Permite el registro, autenticación y autorización de usuarios.
*   **Responsabilidades:** Autenticación (JWT/Sesiones), almacenamiento seguro de credenciales (Hash) y, fundamentalmente, la configuración y almacenamiento de la zona horaria preferida por el usuario para la correcta visualización de eventos.

### 2. Módulo de Extracción e Integración de Datos (Prioridad: Alta)
*   **Descripción:** Componente clave para resolver la problemática principal del proyecto.
*   **Responsabilidades:** Consumir APIs externas de las categorías soportadas para recopilar datos (tiempos, fechas, posiciones y participantes), validar y normalizar sus respuestas, y persistirlas en el esquema genérico de PostgreSQL. Debe contemplar errores de disponibilidad, límites de consumo y diferencias entre los formatos de cada proveedor.

### 3. Módulo Core de Gestión Deportiva (Prioridad: Alta)
*   **Descripción:** El núcleo de la aplicación. Maneja el ABM (Alta, Baja, Modificación) y la consulta de las entidades principales.
*   **Responsabilidades:** Gestión de Categorías (F1, WRC, TC), Temporadas, Pilotos, Equipos, Eventos, Sesiones, Etapas, Tripulaciones, Resultados y palmarés. Este módulo expone la información que el frontend consumirá para calendarios, tablas de posiciones y perfiles.

### 4. Módulo de Calendario Unificado (Prioridad: Media)
*   **Descripción:** Encargado de la gestión del tiempo y la planificación de los eventos.
*   **Responsabilidades:** Calcular e inyectar la diferencia horaria entre el servidor/circuito y la zona horaria del usuario logueado. Generar y exportar archivos `.ics` o enlaces dinámicos para vincular los horarios de las carreras a calendarios móviles (Android/iOS).

### 5. Módulo Estadístico y Visual (Prioridad: Baja - Etapa final del MVP)
*   **Descripción:** Agrega el valor analítico diferencial del proyecto.
*   **Responsabilidades:** Procesar los resultados almacenados en el módulo Core para generar estructuras de datos agregadas. Alimentará los gráficos del frontend proporcionando información sobre: evolución de posiciones en el campeonato, tendencias de rendimiento por equipo/marca y comparativas históricas.

### 6. Módulo de Telemetría en Vivo (Funcionalidad plus; prioridad baja)
*   **Descripción:** Extensión opcional del MVP para enriquecer la experiencia de seguimiento de Fórmula 1.
*   **Responsabilidades:** Consumir una API externa que provea telemetría en vivo de F1 y presentar los datos disponibles durante una sesión. El alcance se limita exclusivamente a Fórmula 1 y depende de la disponibilidad, condiciones de uso y límites de consumo del proveedor; no se considera funcionalidad base para WRC ni Turismo Carretera.