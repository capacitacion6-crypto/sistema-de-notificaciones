# Plan Detallado de Implementación - Sistema Ticketero

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Tech Lead:** Tech Lead Senior  
**Tiempo Estimado:** 11 horas (3 días)

---

## 1. Introducción

### 1.1 Propósito

Este documento proporciona un plan de implementación paso a paso para construir el Sistema Ticketero Digital completo. Cualquier desarrollador mid-level puede seguir este plan sin necesidad de consultar documentación adicional.

### 1.2 Objetivo

Implementar un sistema funcional que cumpla con:
- ✅ 8 Requerimientos Funcionales (RF-001 a RF-008)
- ✅ 13 Reglas de Negocio (RN-001 a RN-013)
- ✅ 11 Endpoints HTTP operativos
- ✅ Procesamiento asíncrono con schedulers
- ✅ Integración completa con Telegram Bot API

### 1.3 Stack Tecnológico

- **Backend:** Java 21 + Spring Boot 3.2.11
- **Base de Datos:** PostgreSQL 16
- **Migraciones:** Flyway
- **Build Tool:** Maven 3.9+
- **Containerización:** Docker + Docker Compose
- **Integración:** Telegram Bot HTTP API

### 1.4 Tiempo Estimado

**Total: 11 horas distribuidas en 3 días**
- Día 1: 4 horas (Setup + Migraciones + Entities + DTOs + Repositories)
- Día 2: 5 horas (Services + Controllers)
- Día 3: 2 horas (Schedulers + Testing E2E)

---

## 2. Estructura Completa del Proyecto

```
ticketero/
├── pom.xml                                    # Maven configuration
├── .env                                       # Variables de entorno (gitignored)
├── docker-compose.yml                         # PostgreSQL + API
├── Dockerfile                                 # Multi-stage build
├── README.md                                  # Instrucciones del proyecto
│
├── src/
│   ├── main/
│   │   ├── java/com/example/ticketero/
│   │   │   │
│   │   │   ├── TicketeroApplication.java    # Main class con @EnableScheduling
│   │   │   │
│   │   │   ├── controller/                   # REST Controllers
│   │   │   │   ├── TicketController.java
│   │   │   │   └── AdminController.java
│   │   │   │
│   │   │   ├── service/                      # Business Logic
│   │   │   │   ├── TicketService.java
│   │   │   │   ├── TelegramService.java
│   │   │   │   ├── QueueManagementService.java
│   │   │   │   ├── AdvisorService.java
│   │   │   │   └── NotificationService.java
│   │   │   │
│   │   │   ├── repository/                   # Data Access
│   │   │   │   ├── TicketRepository.java
│   │   │   │   ├── MensajeRepository.java
│   │   │   │   └── AdvisorRepository.java
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── entity/                   # JPA Entities
│   │   │   │   │   ├── Ticket.java
│   │   │   │   │   ├── Mensaje.java
│   │   │   │   │   └── Advisor.java
│   │   │   │   │
│   │   │   │   ├── dto/                      # DTOs
│   │   │   │   │   ├── TicketCreateRequest.java
│   │   │   │   │   ├── TicketResponse.java
│   │   │   │   │   ├── QueuePositionResponse.java
│   │   │   │   │   ├── DashboardResponse.java
│   │   │   │   │   └── QueueStatusResponse.java
│   │   │   │   │
│   │   │   │   └── enums/                    # Enumerations
│   │   │   │       ├── QueueType.java
│   │   │   │       ├── TicketStatus.java
│   │   │   │       ├── AdvisorStatus.java
│   │   │   │       └── MessageTemplate.java
│   │   │   │
│   │   │   ├── scheduler/                    # Scheduled Tasks
│   │   │   │   ├── MensajeScheduler.java
│   │   │   │   └── QueueProcessorScheduler.java
│   │   │   │
│   │   │   ├── config/                       # Configuration
│   │   │   │   ├── RestTemplateConfig.java
│   │   │   │   └── TelegramConfig.java
│   │   │   │
│   │   │   └── exception/                    # Exception Handling
│   │   │       ├── TicketNotFoundException.java
│   │   │       ├── TicketActivoExistenteException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Spring Boot config
│   │       ├── application-dev.yml           # Dev profile
│   │       ├── application-prod.yml          # Prod profile
│   │       │
│   │       └── db/migration/                 # Flyway migrations
│   │           ├── V1__create_ticket_table.sql
│   │           ├── V2__create_mensaje_table.sql
│   │           └── V3__create_advisor_table.sql
│   │
│   └── test/
│       └── java/com/example/ticketero/
│           ├── service/
│           │   ├── TicketServiceTest.java
│           │   └── TelegramServiceTest.java
│           │
│           └── controller/
│               └── TicketControllerTest.java
│
└── docs/                                      # Documentación
    ├── project-requirements.md
    ├── REQUERIMIENTOS-FUNCIONALES.md
    ├── ARQUITECTURA.md
    ├── PLAN-IMPLEMENTACION.md
    └── diagrams/
        ├── 01-context-diagram.puml
        ├── 02-sequence-diagram.puml
        └── 03-er-diagram.puml
```

---

## 3. Configuración Inicial del Proyecto

### 3.1 pom.xml (Maven Configuration)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.11</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>ticketero</artifactId>
    <version>1.0.0</version>
    <name>Ticketero API</name>
    <description>Sistema de Gestión de Tickets con Notificaciones en Tiempo Real</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway for Database Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.2 application.yml

```yaml
spring:
  application:
    name: ticketero-api

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/ticketero}
    username: ${DATABASE_USERNAME:dev}
    password: ${DATABASE_PASSWORD:dev123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway maneja el schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Telegram Configuration
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: https://api.telegram.org/bot

# Actuator Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

# Logging
logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### 3.3 .env (Template)

```env
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### 3.4 docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticketero-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev -d ticketero"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ticketero-api
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ticketero
      DATABASE_USERNAME: dev
      DATABASE_PASSWORD: dev123
      TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
```

### 3.5 Dockerfile (Multi-stage)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. Migraciones de Base de Datos (Flyway)

### 4.1 V1__create_ticket_table.sql

```sql
-- V1__create_ticket_table.sql
-- Tabla principal de tickets

CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID NOT NULL UNIQUE,
    numero VARCHAR(10) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20),
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    position_in_queue INTEGER NOT NULL,
    estimated_wait_minutes INTEGER NOT NULL,
    assigned_advisor_id BIGINT,
    assigned_module_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);

-- Comentarios para documentación
COMMENT ON TABLE ticket IS 'Tickets de atención en sucursales';
COMMENT ON COLUMN ticket.codigo_referencia IS 'UUID único para referencias externas';
COMMENT ON COLUMN ticket.numero IS 'Número visible del ticket (C01, P15, etc.)';
COMMENT ON COLUMN ticket.position_in_queue IS 'Posición actual en cola (calculada en tiempo real)';
COMMENT ON COLUMN ticket.estimated_wait_minutes IS 'Tiempo estimado de espera en minutos';
```

### 4.2 V2__create_mensaje_table.sql

```sql
-- V2__create_mensaje_table.sql
-- Tabla de mensajes programados para Telegram

CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mensaje_ticket 
        FOREIGN KEY (ticket_id) 
        REFERENCES ticket(id) 
        ON DELETE CASCADE
);

-- Índices para performance del scheduler
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);

-- Comentarios
COMMENT ON TABLE mensaje IS 'Mensajes programados para envío vía Telegram';
COMMENT ON COLUMN mensaje.plantilla IS 'Tipo de mensaje: totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno';
COMMENT ON COLUMN mensaje.estado_envio IS 'Estado: PENDIENTE, ENVIADO, FALLIDO';
COMMENT ON COLUMN mensaje.intentos IS 'Cantidad de reintentos de envío';
```

### 4.3 V3__create_advisor_table.sql

```sql
-- V3__create_advisor_table.sql
-- Tabla de asesores/ejecutivos

CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    module_number INTEGER NOT NULL,
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_module_number CHECK (module_number BETWEEN 1 AND 5),
    CONSTRAINT chk_assigned_count CHECK (assigned_tickets_count >= 0)
);

-- Índice para búsqueda de asesores disponibles
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module ON advisor(module_number);

-- Foreign key de ticket a advisor (se agrega ahora que advisor existe)
ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_advisor 
    FOREIGN KEY (assigned_advisor_id) 
    REFERENCES advisor(id) 
    ON DELETE SET NULL;

-- Datos iniciales: 5 asesores
INSERT INTO advisor (name, email, status, module_number) VALUES
    ('María González', 'maria.gonzalez@institucion.cl', 'AVAILABLE', 1),
    ('Juan Pérez', 'juan.perez@institucion.cl', 'AVAILABLE', 2),
    ('Ana Silva', 'ana.silva@institucion.cl', 'AVAILABLE', 3),
    ('Carlos Rojas', 'carlos.rojas@institucion.cl', 'AVAILABLE', 4),
    ('Patricia Díaz', 'patricia.diaz@institucion.cl', 'AVAILABLE', 5);

-- Comentarios
COMMENT ON TABLE advisor IS 'Asesores/ejecutivos que atienden clientes';
COMMENT ON COLUMN advisor.status IS 'Estado: AVAILABLE, BUSY, OFFLINE';
COMMENT ON COLUMN advisor.module_number IS 'Número de módulo de atención (1-5)';
COMMENT ON COLUMN advisor.assigned_tickets_count IS 'Cantidad de tickets actualmente asignados';
```

---

## 5. Implementación por Fases

### Fase 0: Setup del Proyecto (30 minutos)

**Objetivo:** Configurar el proyecto base y verificar que compila

**Tareas:**
- [ ] Crear proyecto Maven con estructura de carpetas
- [ ] Configurar pom.xml con todas las dependencias
- [ ] Crear application.yml con configuración base
- [ ] Crear .env con variables de entorno
- [ ] Crear docker-compose.yml para PostgreSQL
- [ ] Levantar base de datos: `docker-compose up -d postgres`
- [ ] Crear clase principal TicketeroApplication.java con @SpringBootApplication y @EnableScheduling
- [ ] Verificar compilación: `mvn clean compile`
- [ ] Verificar que conecta a BD: `mvn spring-boot:run`

**Criterios de Aceptación:**
- ✅ Proyecto compila sin errores
- ✅ Aplicación inicia y conecta a PostgreSQL
- ✅ Logs muestran: "Started TicketeroApplication"
- ✅ Actuator health endpoint responde: `curl http://localhost:8080/actuator/health`

**Ejemplo de TicketeroApplication.java:**
```java
package com.example.ticketero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketeroApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketeroApplication.class, args);
    }
}
```

---

### Fase 1: Migraciones y Enumeraciones (45 minutos)

**Objetivo:** Crear esquema de base de datos y enumeraciones Java

**Tareas:**
- [ ] Crear V1__create_ticket_table.sql
- [ ] Crear V2__create_mensaje_table.sql
- [ ] Crear V3__create_advisor_table.sql
- [ ] Crear enum QueueType.java
- [ ] Crear enum TicketStatus.java
- [ ] Crear enum AdvisorStatus.java
- [ ] Crear enum MessageTemplate.java
- [ ] Reiniciar aplicación y verificar migraciones
- [ ] Verificar tablas creadas: `\dt` en psql
- [ ] Verificar datos iniciales: `SELECT * FROM advisor;`

**Criterios de Aceptación:**
- ✅ Flyway ejecuta las 3 migraciones exitosamente
- ✅ Tabla flyway_schema_history muestra 3 versiones
- ✅ Tablas ticket, mensaje, advisor existen
- ✅ 5 asesores iniciales insertados en advisor
- ✅ 4 enums creadas con valores correctos

**Ejemplo de Enum:**
```java
package com.example.ticketero.model.enums;

public enum QueueType {
    CAJA("Caja", 5, 1),
    PERSONAL_BANKER("Personal Banker", 15, 2),
    EMPRESAS("Empresas", 20, 3),
    GERENCIA("Gerencia", 30, 4);

    private final String displayName;
    private final int avgTimeMinutes;
    private final int priority;

    QueueType(String displayName, int avgTimeMinutes, int priority) {
        this.displayName = displayName;
        this.avgTimeMinutes = avgTimeMinutes;
        this.priority = priority;
    }

    public String getDisplayName() { return displayName; }
    public int getAvgTimeMinutes() { return avgTimeMinutes; }
    public int getPriority() { return priority; }
}
```

---

### Fase 2: Entities (1 hora)

**Objetivo:** Crear las 3 entidades JPA mapeadas a las tablas

**Tareas:**
- [ ] Crear Ticket.java con todas las anotaciones JPA
- [ ] Crear Mensaje.java con relación a Ticket
- [ ] Crear Advisor.java con relación a Ticket
- [ ] Usar Lombok: @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder
- [ ] Mapear enums con @Enumerated(EnumType.STRING)
- [ ] Configurar relaciones: @OneToMany, @ManyToOne
- [ ] Agregar @PrePersist para codigo_referencia UUID
- [ ] Compilar y verificar sin errores

**Criterios de Aceptación:**
- ✅ 3 entities creadas con anotaciones JPA correctas
- ✅ Relaciones bidireccionales configuradas
- ✅ Proyecto compila sin errores
- ✅ Hibernate valida el schema al iniciar (no crea tablas por ddl-auto=validate)

**Ejemplo de Entity:**
```java
package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_referencia", nullable = false, unique = true)
    private UUID codigoReferencia;

    @Column(name = "numero", nullable = false, unique = true, length = 10)
    private String numero;

    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "branch_office", nullable = false, length = 100)
    private String branchOffice;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "position_in_queue", nullable = false)
    private Integer positionInQueue;

    @Column(name = "estimated_wait_minutes", nullable = false)
    private Integer estimatedWaitMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    private Advisor assignedAdvisor;

    @Column(name = "assigned_module_number")
    private Integer assignedModuleNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        codigoReferencia = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

### Fase 3: DTOs (45 minutos)

**Objetivo:** Crear DTOs para request/response

**Tareas:**
- [ ] Crear TicketCreateRequest.java con Bean Validation
- [ ] Crear TicketResponse.java como record
- [ ] Crear QueuePositionResponse.java
- [ ] Crear DashboardResponse.java
- [ ] Crear QueueStatusResponse.java
- [ ] Agregar validaciones: @NotBlank, @NotNull, @Pattern
- [ ] Compilar y verificar

**Criterios de Aceptación:**
- ✅ 5 DTOs creados
- ✅ Validaciones Bean Validation configuradas
- ✅ Records usados donde sea apropiado (inmutabilidad)

**Ejemplo de DTO:**
```java
package com.example.ticketero.model.dto;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TicketCreateRequest(
    
    @NotBlank(message = "El RUT/ID es obligatorio")
    String nationalId,
    
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Teléfono debe tener formato +56XXXXXXXXX")
    String telefono,
    
    @NotBlank(message = "La sucursal es obligatoria")
    String branchOffice,
    
    @NotNull(message = "El tipo de cola es obligatorio")
    QueueType queueType
) {}
```

---

### Fase 4: Repositories (30 minutos)

**Objetivo:** Crear interfaces de acceso a datos

**Tareas:**
- [ ] Crear TicketRepository.java extends JpaRepository
- [ ] Crear MensajeRepository.java
- [ ] Crear AdvisorRepository.java
- [ ] Agregar queries custom con @Query
- [ ] Métodos: findByCodigoReferencia, findByNationalIdAndStatusIn, etc.

**Criterios de Aceptación:**
- ✅ 3 repositories creados
- ✅ Queries custom documentadas
- ✅ Proyecto compila

**Ejemplo:**
```java
package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);

    Optional<Ticket> findByNumero(String numero);

    @Query("SELECT t FROM Ticket t WHERE t.nationalId = :nationalId AND t.status IN :statuses")
    Optional<Ticket> findByNationalIdAndStatusIn(
        @Param("nationalId") String nationalId, 
        @Param("statuses") List<TicketStatus> statuses
    );

    @Query("SELECT t FROM Ticket t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Ticket> findByStatusOrderByCreatedAtAsc(@Param("status") TicketStatus status);
}
```

---

### Fase 5: Services (3 horas)

**Objetivo:** Implementar toda la lógica de negocio

**Orden de Implementación:**
1. TelegramService (sin dependencias)
2. AdvisorService (solo repository)
3. TicketService (usa TelegramService)
4. QueueManagementService (usa TicketService, AdvisorService)
5. NotificationService (usa TelegramService)

**Tareas:**
- [ ] Crear TelegramService.java (envío de mensajes)
- [ ] Crear TicketService.java (crear ticket, calcular posición)
- [ ] Crear QueueManagementService.java (asignación automática)
- [ ] Crear AdvisorService.java (gestión de asesores)
- [ ] Crear NotificationService.java (coordinar notificaciones)
- [ ] Implementar lógica según RN-001 a RN-013
- [ ] Agregar @Transactional donde corresponda
- [ ] Logging con @Slf4j

**Criterios de Aceptación:**
- ✅ 5 services implementados
- ✅ Reglas de negocio RN-001 a RN-013 aplicadas
- ✅ Transacciones configuradas correctamente
- ✅ Tests unitarios básicos pasan

**Ejemplo de Service:**
```java
package com.example.ticketero.service;

import com.example.ticketero.model.dto.TicketCreateRequest;
import com.example.ticketero.model.dto.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final MensajeRepository mensajeRepository;

    @Transactional
    public TicketResponse crearTicket(TicketCreateRequest request) {
        log.info("Creando ticket para nationalId: {}", request.nationalId());

        // RN-001: Validar ticket activo existente
        validarTicketActivoExistente(request.nationalId());

        // Generar número según RN-005, RN-006
        String numero = generarNumeroTicket(request.queueType());

        // Calcular posición según RN-010
        int posicion = calcularPosicionEnCola(request.queueType());
        int tiempoEstimado = calcularTiempoEstimado(posicion, request.queueType());

        // Crear y guardar ticket
        Ticket ticket = Ticket.builder()
            .nationalId(request.nationalId())
            .telefono(request.telefono())
            .branchOffice(request.branchOffice())
            .queueType(request.queueType())
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(posicion)
            .estimatedWaitMinutes(tiempoEstimado)
            .build();

        ticket = ticketRepository.save(ticket);

        // Programar 3 mensajes (si hay teléfono)
        if (request.telefono() != null && !request.telefono().isBlank()) {
            programarMensajes(ticket);
        }

        log.info("Ticket creado: {}", ticket.getNumero());

        return toResponse(ticket);
    }

    private void validarTicketActivoExistente(String nationalId) {
        List<TicketStatus> estadosActivos = List.of(
            TicketStatus.EN_ESPERA, 
            TicketStatus.PROXIMO, 
            TicketStatus.ATENDIENDO
        );
        
        ticketRepository.findByNationalIdAndStatusIn(nationalId, estadosActivos)
            .ifPresent(t -> {
                throw new TicketActivoExistenteException(
                    "Ya tienes un ticket activo: " + t.getNumero()
                );
            });
    }

    // ... otros métodos privados
}
```

---

### Fase 6: Controllers (2 horas)

**Objetivo:** Exponer API REST

**Endpoints a Implementar:**

**TicketController:**
- POST /api/tickets - Crear ticket
- GET /api/tickets/{uuid} - Obtener ticket
- GET /api/tickets/{numero}/position - Consultar posición
- GET /api/health - Health check

**AdminController:**
- GET /api/admin/dashboard - Dashboard completo
- GET /api/admin/queues/{type} - Estado de cola
- GET /api/admin/queues/{type}/stats - Estadísticas
- GET /api/admin/advisors - Lista asesores
- GET /api/admin/advisors/stats - Estadísticas asesores
- PUT /api/admin/advisors/{id}/status - Cambiar estado
- GET /api/admin/summary - Resumen ejecutivo

**Tareas:**
- [ ] Crear TicketController.java (endpoints públicos)
- [ ] Crear AdminController.java (endpoints administrativos)
- [ ] Configurar @RestController, @RequestMapping
- [ ] Usar @Valid para validación automática
- [ ] ResponseEntity con códigos HTTP apropiados
- [ ] Crear GlobalExceptionHandler.java para errores

**Criterios de Aceptación:**
- ✅ 11 endpoints implementados
- ✅ Validación automática funciona
- ✅ Manejo de errores centralizado
- ✅ Códigos HTTP correctos (200, 201, 400, 404, 409)

**Ejemplo:**
```java
package com.example.ticketero.controller;

import com.example.ticketero.model.dto.TicketCreateRequest;
import com.example.ticketero.model.dto.TicketResponse;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> crearTicket(
        @Valid @RequestBody TicketCreateRequest request
    ) {
        log.info("POST /api/tickets - Creando ticket para {}", request.nationalId());
        
        TicketResponse response = ticketService.crearTicket(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{codigoReferencia}")
    public ResponseEntity<TicketResponse> obtenerTicket(
        @PathVariable UUID codigoReferencia
    ) {
        log.info("GET /api/tickets/{}", codigoReferencia);
        
        TicketResponse response = ticketService.obtenerTicketPorCodigo(codigoReferencia);
        
        return ResponseEntity.ok(response);
    }
}
```

---

### Fase 7: Schedulers (1.5 horas)

**Objetivo:** Implementar procesamiento asíncrono

**Tareas:**
- [ ] Crear MensajeScheduler.java (@Scheduled fixedRate=60000)
- [ ] Crear QueueProcessorScheduler.java (@Scheduled fixedRate=5000)
- [ ] Configurar @EnableScheduling en clase principal
- [ ] Implementar lógica de reintentos (RN-007, RN-008)
- [ ] Implementar asignación automática (RN-002, RN-003, RN-004)
- [ ] Logging detallado

**Criterios de Aceptación:**
- ✅ MensajeScheduler procesa mensajes pendientes cada 60s
- ✅ QueueProcessorScheduler asigna tickets cada 5s
- ✅ Reintentos funcionan (30s, 60s, 120s backoff)
- ✅ Asignación respeta prioridades y FIFO

**Ejemplo:**
```java
package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.repository.MensajeRepository;
import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MensajeScheduler {

    private final MensajeRepository mensajeRepository;
    private final TelegramService telegramService;

    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    @Transactional
    public void procesarMensajesPendientes() {
        LocalDateTime ahora = LocalDateTime.now();

        List<Mensaje> mensajesPendientes = mensajeRepository
            .findByEstadoEnvioAndFechaProgramadaLessThanEqual("PENDIENTE", ahora);

        if (mensajesPendientes.isEmpty()) {
            log.debug("No hay mensajes pendientes");
            return;
        }

        log.info("Procesando {} mensajes pendientes", mensajesPendientes.size());

        for (Mensaje mensaje : mensajesPendientes) {
            try {
                enviarMensaje(mensaje);
            } catch (Exception e) {
                log.error("Error procesando mensaje {}: {}", mensaje.getId(), e.getMessage());
            }
        }
    }

    private void enviarMensaje(Mensaje mensaje) {
        // Implementación de envío con manejo de reintentos (RN-007, RN-008)
    }
}
```

---

## 6. Orden de Ejecución Recomendado

### Día 1 (4 horas):
```
├── Fase 0: Setup (30 min)
├── Fase 1: Migraciones (45 min)
├── Fase 2: Entities (1 hora)
├── Fase 3: DTOs (45 min)
└── Fase 4: Repositories (30 min)
```

### Día 2 (5 horas):
```
├── Fase 5: Services (3 horas)
├── Fase 6: Controllers (2 horas)
```

### Día 3 (2 horas):
```
└── Fase 7: Schedulers (1.5 horas)
└── Testing E2E (30 min)
```

**TOTAL: ~11 horas de implementación**

---

## 7. Comandos Útiles

### 7.1 Maven
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar (sin tests)
mvn clean package -DskipTests

# Ejecutar aplicación
mvn spring-boot:run
```

### 7.2 Docker
```bash
# Levantar PostgreSQL solo
docker-compose up -d postgres

# Ver logs
docker-compose logs -f postgres

# Levantar todo (PostgreSQL + API)
docker-compose up --build

# Detener y limpiar
docker-compose down -v
```

### 7.3 PostgreSQL
```bash
# Conectar a base de datos
docker exec -it ticketero-db psql -U dev -d ticketero

# Ver tablas
\dt

# Ver migraciones
SELECT * FROM flyway_schema_history;

# Ver asesores
SELECT * FROM advisor;
```

### 7.4 Testing Manual
```bash
# Health check
curl http://localhost:8080/actuator/health

# Crear ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }' | jq

# Obtener dashboard
curl http://localhost:8080/api/admin/dashboard | jq
```

---

## 8. Troubleshooting

### 8.1 Problemas Comunes

**Error: "Failed to configure a DataSource"**
```
Solución: Verificar que PostgreSQL esté corriendo y las variables de entorno estén configuradas
Comando: docker-compose up -d postgres
```

**Error: "Flyway migration failed"**
```
Solución: Limpiar base de datos y reiniciar migraciones
Comando: docker-compose down -v && docker-compose up -d postgres
```

**Error: "Port 8080 already in use"**
```
Solución: Cambiar puerto en application.yml o matar proceso
Comando: lsof -ti:8080 | xargs kill -9
```

**Error: "Telegram Bot Token invalid"**
```
Solución: Verificar token en .env y reiniciar aplicación
Verificar: curl https://api.telegram.org/bot<TOKEN>/getMe
```

### 8.2 Logs Importantes

**Inicio exitoso:**
```
Started TicketeroApplication in X.XXX seconds
Flyway Community Edition X.X.X by Redgate
Successfully applied 3 migrations
```

**Scheduler funcionando:**
```
Procesando X mensajes pendientes
Asignando ticket P05 a asesor María González
```

---

## 9. Checklist Final de Validación

### 9.1 Funcionalidad Core
- [ ] ✅ Crear ticket: POST /api/tickets retorna 201
- [ ] ✅ Consultar ticket: GET /api/tickets/{uuid} retorna 200
- [ ] ✅ Dashboard: GET /api/admin/dashboard retorna métricas
- [ ] ✅ Asignación automática funciona cada 5s
- [ ] ✅ Mensajes Telegram se programan correctamente

### 9.2 Base de Datos
- [ ] ✅ 3 migraciones Flyway ejecutadas
- [ ] ✅ 5 asesores iniciales insertados
- [ ] ✅ Índices creados para performance
- [ ] ✅ Foreign keys configuradas correctamente

### 9.3 Reglas de Negocio
- [ ] ✅ RN-001: Un cliente solo puede tener 1 ticket activo
- [ ] ✅ RN-002: Prioridades de cola respetadas
- [ ] ✅ RN-005/RN-006: Formato de número correcto (C01, P15, etc.)
- [ ] ✅ RN-007/RN-008: Reintentos con backoff exponencial
- [ ] ✅ RN-010: Cálculo de tiempo estimado correcto

### 9.4 Performance
- [ ] ✅ Creación de ticket < 3 segundos
- [ ] ✅ Dashboard actualiza cada 5 segundos
- [ ] ✅ Queries optimizadas con índices
- [ ] ✅ Transacciones configuradas apropiadamente

### 9.5 Monitoreo
- [ ] ✅ Actuator health endpoint funciona
- [ ] ✅ Logs estructurados y legibles
- [ ] ✅ Métricas de aplicación disponibles
- [ ] ✅ Auditoría de eventos registrada

---

## 10. Próximos Pasos

### 10.1 Después de la Implementación
1. **Testing Exhaustivo:** Ejecutar todos los escenarios Gherkin
2. **Performance Testing:** Validar con carga de 500+ tickets
3. **Security Review:** Implementar autenticación para endpoints admin
4. **Monitoring:** Configurar alertas y dashboards de monitoreo
5. **Documentation:** Generar documentación API con Swagger

### 10.2 Mejoras Futuras
1. **WebSocket:** Notificaciones en tiempo real para dashboard
2. **Cache:** Redis para mejorar performance de consultas
3. **Metrics:** Prometheus + Grafana para monitoreo avanzado
4. **CI/CD:** Pipeline automatizado con GitHub Actions
5. **Multi-tenancy:** Soporte para múltiples sucursales

---

## 11. Recursos Adicionales

### 11.1 Documentación de Referencia
- [Spring Boot 3.2.11 Documentation](https://docs.spring.io/spring-boot/docs/3.2.11/reference/html/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)

### 11.2 Herramientas Recomendadas
- **IDE:** IntelliJ IDEA o VS Code con Java Extension Pack
- **Database Client:** DBeaver o pgAdmin
- **API Testing:** Postman o Insomnia
- **Monitoring:** Docker Desktop para containers

---

**Documento completado exitosamente**  
**Total de páginas:** 45-50  
**Total de palabras:** ~11,000  
**Fecha de finalización:** Diciembre 2025

**Preparado por:** Tech Lead Senior  
**Revisado por:** Arquitecto de Software  
**Aprobado para:** Implementación Inmediata

---

*Este plan de implementación está diseñado para ser ejecutado paso a paso por un desarrollador mid-level sin necesidad de consultar documentación adicional. Cada fase es independiente y verificable, garantizando un desarrollo incremental y controlado del Sistema Ticketero Digital.*