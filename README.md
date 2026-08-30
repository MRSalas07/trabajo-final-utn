# Motorsport Analytics

Plataforma web dedicada a la integración, consulta y análisis de información de diversas categorías de automovilismo deportivo (Motorsport).

> **Trabajo Final Integrador (TFI)** — Tecnicatura Universitaria en Programación  
> **Universidad Tecnológica Nacional (UTN)**  
> **Grupo:** 190  
> **Integrantes:** Salas Mateo Roman · Luna Genaro Nahuel  
> **Hito:** Entrega 1 — Propuesta de Idea y Alcance  
> **Fecha de entrega:** 30/08/2026

## Repositorio

[GitHub — trabajo-final-utn](https://github.com/MRSalas07/trabajo-final-utn)

---

## Descripción del proyecto

**Motorsport Analytics** es una plataforma web dedicada a la integración, consulta y análisis de información de diversas categorías de automovilismo deportivo.

Su propósito principal es **recopilar, normalizar, almacenar y presentar de manera unificada** los datos deportivos que actualmente se encuentran fragmentados y dispersos en la web, ofreciendo una experiencia limpia e intuitiva inspirada en plataformas deportivas de fútbol como Promiedos o 365scores.

## Problemática

En el ecosistema del automovilismo deportivo, tanto nacional como internacional, la información sobre calendarios, horarios de eventos, estadísticas de rendimiento e historiales se encuentra descentralizada y distribuida entre múltiples plataformas oficiales y medios especializados de difícil lectura.

Cada categoría opera bajo su propia estructura de datos, nomenclatura y formatos dispares, frecuentemente archivos PDF de difícil acceso o sitios poco amigables en dispositivos móviles.

Esto genera una ineficiencia para el fanático y el analista deportivo, quienes deben consultar múltiples fuentes desconectadas para obtener un panorama unificado del Motorsport.

## Justificación y valor agregado

El proyecto se justifica técnicamente al resolver esta fragmentación mediante una **capa de integración que centraliza y normaliza datos provenientes de diversas fuentes**.

El factor diferencial respecto a un sistema CRUD tradicional radica en el procesamiento estadístico propio de los datos crudos recopilados.

La plataforma permitirá visualizar información estadística por la categoría seleccionada, tales como:

- Posiciones en el campeonato.
- Equipos.
- Marcas.
- Fechas disputadas y por disputarse.
- Información estadística y comparativa.

Esto transformará datos estáticos en información visual y analítica de alto valor dinámico.

También se prevé agregar la opción de **vincular la carrera que se desea notificar por fecha al calendario del celular**, ya sea Android o iOS.

## Alcance inicial — MVP

Con el fin de garantizar la viabilidad del proyecto dentro del cronograma académico, el alcance del MVP se delimita estrictamente a la integración de tres categorías contrastantes de automovilismo:

### Fórmula 1 (F1)

Categoría reina internacional de circuitos, con esquemas de sesiones tradicionales de entrenamientos, clasificación y carreras.

### World Rally Championship (WRC)

Categoría internacional con formato de tramos cronometrados (Súper Especiales) y enlaces.

### Turismo Carretera (TC)

Categoría de pista altamente representativa del automovilismo nacional argentino, con series clasificatorias y carreras finales.

## Funcionalidades previstas

Las características del sistema que serán implementadas en el MVP corresponden a:

### Consulta de categorías

Selección y visualización del perfil de cada una de las tres categorías soportadas.

### Calendario unificado

Consulta de temporadas, fechas de carreras, circuitos/sedes y conversión automática de horarios de carrera según la zona horaria del usuario.

### Resultados e históricos

Visualización de tablas de posiciones finales de cada sesión competitiva.


### Módulo estadístico y visual

Generación de gráficos comparativos para facilitar el análisis del usuario, incluyendo:

- Evolución de posiciones.
- Tendencias de rendimiento.
- Puntajes.

## Stack tecnológico

De acuerdo con el principio de que la familiaridad con la tecnología es el factor prioritario de viabilidad, se propone un stack robusto dominado por el equipo de desarrollo.

| Componente | Tecnología | Rol y justificación técnica |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 3.x | Responsable de la lógica de negocio, persistencia mediante Spring Data JPA, seguridad mediante Spring Security, validación de datos, normalización de información externa y exposición de la API REST. |
| **Frontend** | Angular + TypeScript / HTML / CSS | Desarrollo de la interfaz web responsiva, consumo asíncrono de la API REST mediante HttpClient, navegación y renderizado dinámico de tablas de posiciones y gráficos. |
| **Base de datos** | MySQL / SQL Server | Almacenamiento estructurado de temporadas, pilotos, equipos y resultados. Garantiza la integridad transaccional (ACID) y la consistencia de las complejas relaciones de datos del automovilismo. |
| **Plataforma / Cloud** | Servicio Cloud PaaS (Render / Railway / Vercel) | Alojamiento del backend y despliegue del frontend integrado de manera automática con el repositorio único de GitHub, cumpliendo el requisito de despliegue en la nube. |

---

## Objetivo del MVP

El MVP busca ofrecer una plataforma centralizada para consultar información de **F1, WRC y TC**, evitando que el usuario tenga que recurrir a múltiples fuentes desconectadas.

La propuesta combina:

- Integración y normalización de datos.
- Consulta de información deportiva.
- Calendario unificado.
- Historiales y resultados.
- Perfiles de pilotos y equipos.
- Estadísticas y visualizaciones comparativas.
- Conversión de horarios según la zona horaria del usuario.
- Posibilidad de vincular eventos con el calendario del dispositivo.

## Estado del proyecto

**Hito actual:** Entrega 1 — Propuesta de Idea y Alcance.

Este documento corresponde a la propuesta inicial del proyecto para el Trabajo Final Integrador de la Tecnicatura Universitaria en Programación.

