# Plan Detallado de Implementación - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Líder Técnico:** Desarrollador Senior

---

## 1. Resumen Ejecutivo

Este documento presenta el plan detallado de implementación del Sistema Ticketero Digital, estructurado en **8 sprints de 1 semana cada uno** para completar el desarrollo en **2 meses**.

El plan sigue una metodología ágil con entregas incrementales, priorizando funcionalidades core primero y características avanzadas después. Cada sprint incluye desarrollo, testing y documentación.

**Cronograma Total:** 8 semanas  
**Metodología:** Scrum con sprints de 1 semana  
**Equipo:** 2-3 desarrolladores + 1 QA + 1 DevOps

---

## 2. Estructura del Proyecto

### 2.1 Arquitectura de Paquetes

```
src/main/java/com/example/ticketero/
├── controller/           # @RestController - Capa de presentación
│   ├── TicketController.java
│   └── AdminController.java
├── service/             # @Service - Lógica de negocio
│   ├── TicketService.java
│   ├── TelegramService.java
│   ├── QueueManagementService.java
│   ├── AdvisorService.java
│   └── NotificationService.java
├── repository/          # @Repository - Acceso a datos
│   ├── TicketRepository.java
│   ├── MensajeRepository.java
│   └── AdvisorRepository.java
├── model/
│   ├── entity/          # @Entity - JPA entities
│   │   ├── Ticket.java
│   │   ├── Mensaje.java
│   │   └── Advisor.java
│   └── dto/             # Records - Request/Response DTOs
│       ├── request/
│       │   ├── TicketRequest.java
│       │   └── AdvisorStatusRequest.java
│       └── response/
│           ├── TicketResponse.java
│           ├── QueuePositionResponse.java
│           └── DashboardResponse.java
├── scheduler/           # @Scheduled - Procesamiento asíncrono
│   ├── MessageScheduler.java
│   └── QueueProcessorScheduler.java
├── config/              # @Configuration - Configuración
│   ├── TelegramConfig.java
│   └── SchedulingConfig.java
├── exception/           # Manejo de errores
│   ├── TicketNotFoundException.java
│   ├── DuplicateActiveTicketException.java
│   └── GlobalExceptionHandler.java
└── util/                # Utilidades
    ├── TicketNumberGenerator.java
    └── QueueCalculator.java
```

### 2.2 Recursos de Base de Datos

```
src/main/resources/
├── application.yml      # Configuración Spring Boot
├── application-dev.yml  # Profile desarrollo
├── application-prod.yml # Profile producción
└── db/migration/        # Migraciones Flyway
    ├── V1__create_tables.sql
    ├── V2__create_indexes.sql
    └── V3__insert_sample_data.sql
```

---

## 3. Cronograma de Sprints

### Sprint 1: Fundación del Proyecto (Semana 1)
**Objetivo:** Establecer la base del proyecto con configuración inicial y modelo de datos

**Historias de Usuario:**
- Como desarrollador, necesito la estructura base del proyecto configurada
- Como desarrollador, necesito las entidades JPA creadas y migraciones funcionando
- Como desarrollador, necesito la configuración de Spring Boot operativa

**Tareas Técnicas:**
1. **Configuración Inicial (2 días)**
   - Crear proyecto Maven con Spring Boot 3.2.11
   - Configurar dependencias (Spring Data JPA, PostgreSQL, Flyway, Validation)
   - Configurar application.yml con profiles (dev/prod)
   - Configurar Docker Compose para desarrollo

2. **Modelo de Datos (2 días)**
   - Crear entidades JPA: Ticket, Mensaje, Advisor
   - Implementar enumeraciones: QueueType, TicketStatus, AdvisorStatus, MessageTemplate
   - Crear migraciones Flyway: V1__create_tables.sql
   - Crear índices: V2__create_indexes.sql

3. **Repositories Base (1 día)**
   - Implementar TicketRepository, MensajeRepository, AdvisorRepository
   - Queries básicas con Spring Data JPA
   - Tests unitarios de repositories

**Criterios de Aceptación:**
- ✅ Proyecto compila sin errores
- ✅ Base de datos se crea automáticamente con Flyway
- ✅ Tests de repositories pasan
- ✅ Docker Compose levanta PostgreSQL + API

**Entregables:**
- Proyecto base configurado
- Entidades JPA funcionando
- Migraciones de BD ejecutándose
- README.md con instrucciones de setup

---

### Sprint 2: API Core - Creación de Tickets (Semana 2)
**Objetivo:** Implementar RF-001 (Crear Ticket Digital) con validaciones completas

**Historias de Usuario:**
- Como cliente, quiero crear un ticket digital ingresando mi RUT y seleccionando servicio
- Como sistema, necesito validar que un cliente no tenga tickets activos (RN-001)
- Como sistema, necesito generar números de ticket únicos (RN-005, RN-006)

**Tareas Técnicas:**
1. **DTOs y Validaciones (1 día)**
   - Crear TicketRequest record con Bean Validation
   - Crear TicketResponse record
   - Implementar validaciones: @NotBlank, @Pattern, @Valid

2. **Lógica de Negocio (2 días)**
   - Implementar TicketService.crearTicket()
   - Validar RN-001: único ticket activo por cliente
   - Implementar TicketNumberGenerator (RN-005, RN-006)
   - Implementar QueueCalculator para posición y tiempo estimado (RN-010)

3. **Controller y API (1 día)**
   - Implementar TicketController.crearTicket()
   - Endpoint POST /api/tickets
   - Manejo de errores con @ControllerAdvice

4. **Testing (1 día)**
   - Tests unitarios de TicketService
   - Tests de integración del endpoint
   - Tests de validaciones

**Criterios de Aceptación:**
- ✅ POST /api/tickets crea ticket exitosamente
- ✅ Valida RN-001: rechaza si cliente tiene ticket activo (HTTP 409)
- ✅ Genera número correcto según tipo de cola (C01, P15, etc.)
- ✅ Calcula posición y tiempo estimado correctamente
- ✅ Retorna HTTP 201 con TicketResponse válido

**Entregables:**
- Endpoint POST /api/tickets funcional
- Validaciones de negocio implementadas
- Suite de tests completa
- Documentación de API (Swagger)

---

### Sprint 3: Integración con Telegram (Semana 3)
**Objetivo:** Implementar TelegramService y programación de mensajes

**Historias de Usuario:**
- Como sistema, necesito enviar mensajes a Telegram Bot API
- Como sistema, necesito programar 3 mensajes por ticket creado
- Como sistema, necesito manejar fallos de envío con reintentos (RN-007, RN-008)

**Tareas Técnicas:**
1. **Configuración Telegram (1 día)**
   - Crear TelegramConfig con RestTemplate
   - Configurar telegram.bot-token en application.yml
   - Implementar TelegramService.enviarMensaje()

2. **Plantillas de Mensajes (1 día)**
   - Implementar generación de texto por plantilla
   - Mensajes con emojis y formato HTML
   - Plantillas: totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno

3. **Programación de Mensajes (2 días)**
   - Modificar TicketService para crear 3 mensajes programados
   - Implementar MessageScheduler con @Scheduled(fixedRate = 60000)
   - Lógica de reintentos con backoff exponencial (RN-008)

4. **Testing y Simulación (1 día)**
   - Tests unitarios de TelegramService
   - Mock de Telegram API para tests
   - Tests de MessageScheduler

**Criterios de Aceptación:**
- ✅ TelegramService envía mensajes exitosamente
- ✅ Al crear ticket se programan 3 mensajes automáticamente
- ✅ MessageScheduler procesa mensajes cada 60 segundos
- ✅ Reintentos funcionan según RN-007 y RN-008
- ✅ Mensajes fallidos se marcan como FALLIDO después de 3 intentos

**Entregables:**
- TelegramService funcional
- MessageScheduler operativo
- Plantillas de mensajes implementadas
- Tests de integración con Telegram (mocked)

---

### Sprint 4: Gestión de Colas y Asesores (Semana 4)
**Objetivo:** Implementar RF-004 (Asignación Automática) y RF-005 (Múltiples Colas)

**Historias de Usuario:**
- Como sistema, necesito asignar tickets automáticamente a asesores disponibles
- Como sistema, necesito respetar prioridades de colas (RN-002)
- Como sistema, necesito balancear carga entre asesores (RN-004)

**Tareas Técnicas:**
1. **AdvisorService (1 día)**
   - Implementar CRUD de asesores
   - Métodos: findAvailableAdvisors(), updateStatus()
   - Lógica de balanceo de carga (assignedTicketsCount)

2. **QueueManagementService (2 días)**
   - Implementar asignarSiguienteTicket()
   - Lógica de prioridades (RN-002): GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA
   - Orden FIFO dentro de cada cola (RN-003)
   - Actualización de estados: ticket → ATENDIENDO, advisor → BUSY

3. **QueueProcessorScheduler (1 día)**
   - Implementar @Scheduled(fixedRate = 5000) // cada 5 segundos
   - Recálculo de posiciones en tiempo real
   - Detección de tickets con posición ≤ 3 → status PROXIMO (RN-012)

4. **Testing (1 día)**
   - Tests de QueueManagementService
   - Tests de prioridades y balanceo
   - Tests de QueueProcessorScheduler

**Criterios de Aceptación:**
- ✅ Asignación automática funciona cada 5 segundos
- ✅ Respeta prioridades de colas (GERENCIA primero)
- ✅ Balancea carga entre asesores disponibles
- ✅ Actualiza posiciones en tiempo real
- ✅ Marca tickets como PROXIMO cuando posición ≤ 3

**Entregables:**
- QueueManagementService completo
- QueueProcessorScheduler operativo
- AdvisorService funcional
- Tests de asignación automática

---

### Sprint 5: API de Consultas (Semana 5)
**Objetivo:** Implementar RF-003 (Consultar Posición) y RF-006 (Estado del Ticket)

**Historias de Usuario:**
- Como cliente, quiero consultar la posición actual de mi ticket
- Como cliente, quiero ver el estado actualizado de mi ticket
- Como sistema, necesito calcular posiciones en tiempo real

**Tareas Técnicas:**
1. **DTOs de Consulta (1 día)**
   - Crear QueuePositionResponse record
   - Crear TicketStatusResponse record
   - Validaciones para consultas por UUID y número

2. **Endpoints de Consulta (2 días)**
   - GET /api/tickets/{uuid} - Obtener ticket completo
   - GET /api/tickets/{numero}/position - Consultar posición
   - Implementar cálculo de posición en tiempo real
   - Manejo de tickets no encontrados (HTTP 404)

3. **Optimización de Queries (1 día)**
   - Queries optimizadas para cálculo de posiciones
   - Índices en campos de búsqueda frecuente
   - Cache de posiciones (opcional)

4. **Testing (1 día)**
   - Tests de endpoints de consulta
   - Tests de cálculo de posiciones
   - Tests de performance

**Criterios de Aceptación:**
- ✅ GET /api/tickets/{uuid} retorna ticket completo
- ✅ GET /api/tickets/{numero}/position retorna posición actual
- ✅ Posiciones se calculan en tiempo real
- ✅ Maneja correctamente tickets no encontrados
- ✅ Response time < 1 segundo para consultas

**Entregables:**
- Endpoints de consulta funcionales
- Cálculo de posiciones optimizado
- Tests de performance
- Documentación de API actualizada

---

### Sprint 6: Panel Administrativo (Semana 6)
**Objetivo:** Implementar RF-007 (Dashboard) y funciones administrativas

**Historias de Usuario:**
- Como supervisor, quiero ver un dashboard con estado de todas las colas
- Como supervisor, quiero ver la lista de asesores y su estado
- Como supervisor, quiero cambiar el estado de los asesores

**Tareas Técnicas:**
1. **DTOs del Dashboard (1 día)**
   - Crear DashboardResponse record
   - Crear QueueSummaryResponse record
   - Crear AdvisorSummaryResponse record

2. **AdminController (2 días)**
   - GET /api/admin/dashboard - Dashboard completo
   - GET /api/admin/queues/{type} - Estado de cola específica
   - GET /api/admin/advisors - Lista de asesores
   - PUT /api/admin/advisors/{id}/status - Cambiar estado asesor

3. **Lógica del Dashboard (1 día)**
   - Agregaciones de tickets por estado y cola
   - Cálculo de métricas: tiempo promedio, tickets en espera
   - Actualización cada 5 segundos (RNF-002)

4. **Testing (1 día)**
   - Tests de AdminController
   - Tests de métricas y agregaciones
   - Tests de cambio de estado de asesores

**Criterios de Aceptación:**
- ✅ Dashboard muestra resumen completo de operación
- ✅ Métricas se actualizan cada 5 segundos
- ✅ Permite cambiar estado de asesores
- ✅ Muestra alertas si cola > 15 personas
- ✅ Interface administrativa funcional

**Entregables:**
- AdminController completo
- Dashboard con métricas en tiempo real
- Funciones administrativas
- Tests de panel administrativo

---

### Sprint 7: Auditoría y Logging (Semana 7)
**Objetivo:** Implementar RF-008 (Auditoría) y logging completo

**Historias de Usuario:**
- Como sistema, necesito registrar todos los eventos críticos (RN-011)
- Como administrador, quiero logs detallados para debugging
- Como auditor, necesito trazabilidad completa de operaciones

**Tareas Técnicas:**
1. **Sistema de Auditoría (2 días)**
   - Crear entidad AuditLog
   - Implementar AuditService
   - Registrar eventos: creación, asignación, completado, cambios de estado
   - Migración para tabla de auditoría

2. **Logging Estructurado (1 día)**
   - Configurar Logback con formato JSON
   - Logs por nivel: ERROR, WARN, INFO, DEBUG
   - Correlation IDs para trazabilidad
   - Logs sin datos sensibles

3. **Métricas y Monitoreo (1 día)**
   - Spring Boot Actuator endpoints
   - Health checks personalizados
   - Métricas de performance
   - Preparación para Prometheus

4. **Testing (1 día)**
   - Tests de auditoría
   - Tests de logging
   - Tests de health checks

**Criterios de Aceptación:**
- ✅ Todos los eventos críticos se auditan
- ✅ Logs estructurados y sin datos sensibles
- ✅ Health checks funcionan correctamente
- ✅ Métricas disponibles vía Actuator
- ✅ Trazabilidad completa de operaciones

**Entregables:**
- Sistema de auditoría completo
- Logging estructurado
- Health checks y métricas
- Tests de auditoría y monitoreo

---

### Sprint 8: Testing Final y Deployment (Semana 8)
**Objetivo:** Testing integral, optimización y preparación para producción

**Historias de Usuario:**
- Como usuario, necesito que el sistema sea confiable y performante
- Como DevOps, necesito el sistema listo para deployment en producción
- Como QA, necesito cobertura de tests completa

**Tareas Técnicas:**
1. **Testing Integral (2 días)**
   - Tests de integración end-to-end
   - Tests de carga con JMeter
   - Tests de concurrencia
   - Cobertura de código > 80%

2. **Optimización (1 día)**
   - Optimización de queries SQL
   - Tuning de connection pools
   - Optimización de schedulers
   - Performance profiling

3. **Deployment (1 día)**
   - Dockerfile optimizado (multi-stage build)
   - docker-compose para producción
   - Scripts de deployment
   - Configuración de variables de entorno

4. **Documentación (1 día)**
   - README completo
   - Documentación de API (OpenAPI/Swagger)
   - Manual de deployment
   - Troubleshooting guide

**Criterios de Aceptación:**
- ✅ Cobertura de tests > 80%
- ✅ Performance tests pasan (< 3s creación ticket)
- ✅ Sistema soporta 25,000 tickets/día
- ✅ Deployment automatizado funciona
- ✅ Documentación completa

**Entregables:**
- Sistema completo y testeado
- Deployment automatizado
- Documentación completa
- Sistema listo para producción

---

## 4. Definición de Terminado (DoD)

Para cada sprint, una funcionalidad se considera terminada cuando:

### 4.1 Código
- ✅ Código implementado según estándares del proyecto
- ✅ Code review aprobado por senior developer
- ✅ Sin code smells críticos (SonarQube)
- ✅ Documentación de código (JavaDoc)

### 4.2 Testing
- ✅ Tests unitarios escritos y pasando
- ✅ Tests de integración escritos y pasando
- ✅ Cobertura de código > 70% por sprint
- ✅ Tests de regresión pasando

### 4.3 Funcionalidad
- ✅ Criterios de aceptación cumplidos
- ✅ Demo funcional completada
- ✅ Validación por Product Owner
- ✅ No bugs críticos pendientes

### 4.4 Deployment
- ✅ Funcionalidad deployada en ambiente de desarrollo
- ✅ Smoke tests pasando en dev
- ✅ Configuración documentada
- ✅ Rollback plan definido

---

## 5. Riesgos y Mitigaciones

### 5.1 Riesgos Técnicos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Integración Telegram falla | Media | Alto | Mock service + tests, fallback manual |
| Performance de BD insuficiente | Baja | Medio | Índices optimizados, connection pooling |
| Concurrencia en asignación | Media | Alto | Locks optimistas, tests de concurrencia |
| Schedulers no escalan | Baja | Medio | Configuración ajustable, monitoreo |

### 5.2 Riesgos de Proyecto

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Requerimientos cambian | Alta | Medio | Sprints cortos, feedback continuo |
| Desarrollador se enferma | Media | Alto | Documentación detallada, pair programming |
| Ambiente de desarrollo falla | Baja | Alto | Docker Compose, backup de configuración |
| Retraso en testing | Media | Medio | Testing paralelo al desarrollo |

---

## 6. Métricas de Éxito

### 6.1 Métricas Técnicas
- **Cobertura de código:** > 80%
- **Performance:** Creación de ticket < 3 segundos
- **Disponibilidad:** > 99.5% durante horario de atención
- **Throughput:** Soportar 25,000 tickets/día

### 6.2 Métricas de Calidad
- **Bugs críticos:** 0 en producción
- **Code smells:** < 5 críticos por sprint
- **Deuda técnica:** < 2 horas por sprint
- **Documentación:** 100% de APIs documentadas

### 6.3 Métricas de Proceso
- **Velocity:** Estable entre sprints
- **Sprint completion:** > 90% de historias completadas
- **Defect escape rate:** < 5% bugs llegan a producción
- **Time to market:** 8 semanas exactas

---

## 7. Recursos y Dependencias

### 7.1 Equipo Requerido
- **Tech Lead:** 1 persona (100% dedicación)
- **Desarrolladores:** 2 personas (100% dedicación)
- **QA Engineer:** 1 persona (50% dedicación)
- **DevOps:** 1 persona (25% dedicación)

### 7.2 Infraestructura
- **Desarrollo:** Docker + PostgreSQL local
- **Testing:** Ambiente dedicado con BD separada
- **CI/CD:** GitHub Actions o Jenkins
- **Monitoreo:** Spring Boot Actuator + logs

### 7.3 Dependencias Externas
- **Telegram Bot Token:** Requerido para Sprint 3
- **Ambiente de producción:** Definido para Sprint 8
- **Acceso a BD producción:** Para migraciones finales
- **Certificados SSL:** Para deployment HTTPS

---

## 8. Plan de Comunicación

### 8.1 Ceremonias Scrum
- **Daily Standup:** Lunes a Viernes 9:00 AM (15 min)
- **Sprint Planning:** Lunes inicio de sprint (2 horas)
- **Sprint Review:** Viernes fin de sprint (1 hora)
- **Sprint Retrospective:** Viernes fin de sprint (1 hora)

### 8.2 Reportes
- **Burndown Chart:** Actualizado diariamente
- **Velocity Chart:** Revisado cada sprint
- **Quality Report:** Semanal (cobertura, bugs, performance)
- **Risk Assessment:** Bi-semanal

### 8.3 Stakeholders
- **Product Owner:** Revisión semanal de funcionalidades
- **Arquitecto:** Revisión técnica cada 2 sprints
- **Usuario Final:** Demo cada 2 sprints
- **Management:** Reporte ejecutivo semanal

---

**PLAN DE IMPLEMENTACIÓN COMPLETADO**

**Duración Total:** 8 semanas  
**Metodología:** Scrum ágil  
**Entrega:** Sistema completo listo para producción  
**Próximo paso:** Inicio de Sprint 1 - Fundación del Proyecto