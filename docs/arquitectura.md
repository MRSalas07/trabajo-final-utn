# Arquitectura del Proyecto

Para el desarrollo de **Motorsport Analytics**, se ha optado por una **Arquitectura Monolítica Orientada a Capas (N-Tier)** combinada con el patrón **MVC (Model-View-Controller)** en el backend, exponiendo sus servicios a través de una **API RESTful**. El frontend se desacopla completamente funcionando como una **Single Page Application (SPA)**.

## Justificación de la Arquitectura

Esta decisión arquitectónica garantiza la separación de responsabilidades (Separation of Concerns). Permite que la lógica de presentación resida exclusivamente en el cliente (Angular), mientras que el servidor (Spring Boot) se enfoca en el procesamiento, la seguridad y la persistencia de datos. Al ser un MVP con tres categorías iniciales, un monolito modularizado en capas es la opción más eficiente y mantenible frente a enfoques más complejos (como microservicios), previniendo la sobreingeniería en esta etapa, pero permitiendo escalar horizontalmente si el tráfico de la plataforma lo requiriese a futuro.

## Capas del Backend (Spring Boot)

1. **Capa de Controladores (API REST):** Recibe las peticiones HTTP del cliente, valida los parámetros de entrada y delega la ejecución a la capa de servicios. Retorna respuestas estructuradas en formato JSON.
2. **Capa de Servicios (Lógica de Negocio):** Contiene el núcleo funcional del sistema. Aquí se ejecutan los cálculos de campeonatos, la lógica de conversión de zonas horarias y el procesamiento estadístico.
3. **Capa de Acceso a Datos (Repositorios):** Implementada con Spring Data JPA. Gestiona las consultas y transacciones hacia la base de datos relacional mediante la especificación JPA y Hibernate, abstrayendo las sentencias SQL nativas.

## Tecnologías Definitivas

*   **Backend:** Java 21 + Spring Boot 3.x. Justificación: Tipado estático fuerte, excelente manejo transaccional y robustez en la exposición de APIs.
*   **Base de Datos:** PostgreSQL. Justificación: Motor relacional open-source de nivel empresarial. Ideal para mantener la integridad ACID, manejo nativo de fechas/timestamps complejos (crucial para los calendarios internacionales del automovilismo) y excelente integración con Hibernate.
*   **Frontend:** Angular + TypeScript. Justificación: Arquitectura basada en componentes que facilita la creación de dashboards interactivos, gráficos estadísticos y tablas dinámicas.
*   **Despliegue (Cloud):** PaaS (Render / Railway / Vercel). Justificación: Integración continua (CI/CD) nativa con repositorios de GitHub.