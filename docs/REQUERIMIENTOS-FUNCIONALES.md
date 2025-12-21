# Requerimientos Funcionales - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Cliente:** Institución Financiera  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Analista:** Analista de Negocio Senior

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requerimientos funcionales del Sistema Ticketero Digital, diseñado para modernizar la experiencia de atención en sucursales mediante:
- Digitalización completa del proceso de tickets
- Notificaciones automáticas en tiempo real vía Telegram
- Movilidad del cliente durante la espera
- Asignación inteligente de clientes a ejecutivos
- Panel de monitoreo para supervisión operacional

### 1.2 Alcance

Este documento cubre:
- ✅ 8 Requerimientos Funcionales (RF-001 a RF-008)
- ✅ 13 Reglas de Negocio (RN-001 a RN-013)
- ✅ Criterios de aceptación en formato Gherkin
- ✅ Modelo de datos funcional
- ✅ Matriz de trazabilidad

Este documento NO cubre:
- ❌ Arquitectura técnica (ver documento ARQUITECTURA.md)
- ❌ Tecnologías de implementación
- ❌ Diseño de interfaces de usuario

### 1.3 Definiciones

| Término | Definición |
|---------|------------|
| Ticket | Turno digital asignado a un cliente para ser atendido |
| Cola | Fila virtual de tickets esperando atención |
| Asesor | Ejecutivo bancario que atiende clientes |
| Módulo | Estación de trabajo de un asesor (numerados 1-5) |
| Chat ID | Identificador único de usuario en Telegram |
| UUID | Identificador único universal para tickets |

---

## 2. Reglas de Negocio

Las siguientes reglas de negocio aplican transversalmente a todos los requerimientos funcionales:

**RN-001: Unicidad de Ticket Activo**  
Un cliente solo puede tener 1 ticket activo a la vez. Los estados activos son: EN_ESPERA, PROXIMO, ATENDIENDO. Si un cliente intenta crear un nuevo ticket teniendo uno activo, el sistema debe rechazar la solicitud con error HTTP 409 Conflict.

**RN-002: Prioridad de Colas**  
Las colas tienen prioridades numéricas para asignación automática:
- GERENCIA: prioridad 4 (máxima)
- EMPRESAS: prioridad 3
- PERSONAL_BANKER: prioridad 2
- CAJA: prioridad 1 (mínima)

Cuando un asesor se libera, el sistema asigna primero tickets de colas con mayor prioridad.

**RN-003: Orden FIFO Dentro de Cola**  
Dentro de una misma cola, los tickets se procesan en orden FIFO (First In, First Out). El ticket más antiguo (createdAt menor) se asigna primero.

**RN-004: Balanceo de Carga Entre Asesores**  
Al asignar un ticket, el sistema selecciona el asesor AVAILABLE con menor valor de assignedTicketsCount, distribuyendo equitativamente la carga de trabajo.

**RN-005: Formato de Número de Ticket**  
El número de ticket sigue el formato: [Prefijo][Número secuencial 01-99]
- Prefijo: 1 letra según el tipo de cola
- Número: 2 dígitos, del 01 al 99, reseteado diariamente

Ejemplos: C01, P15, E03, G02

**RN-006: Prefijos por Tipo de Cola**  
- CAJA → C
- PERSONAL_BANKER → P
- EMPRESAS → E
- GERENCIA → G

**RN-007: Reintentos Automáticos de Mensajes**  
Si el envío de un mensaje a Telegram falla, el sistema reintenta automáticamente hasta 3 veces antes de marcarlo como FALLIDO.

**RN-008: Backoff Exponencial en Reintentos**  
Los reintentos de mensajes usan backoff exponencial:
- Intento 1: inmediato
- Intento 2: después de 30 segundos
- Intento 3: después de 60 segundos
- Intento 4: después de 120 segundos

**RN-009: Estados de Ticket**  
Un ticket puede estar en uno de estos estados:
- EN_ESPERA: esperando asignación a asesor
- PROXIMO: próximo a ser atendido (posición ≤ 3)
- ATENDIENDO: siendo atendido por un asesor
- COMPLETADO: atención finalizada exitosamente
- CANCELADO: cancelado por cliente o sistema
- NO_ATENDIDO: cliente no se presentó cuando fue llamado

**RN-010: Cálculo de Tiempo Estimado**  
El tiempo estimado de espera se calcula como:

tiempoEstimado = posiciónEnCola × tiempoPromedioCola

Donde tiempoPromedioCola varía por tipo:
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos

**RN-011: Auditoría Obligatoria**  
Todos los eventos críticos del sistema deben registrarse en auditoría con: timestamp, tipo de evento, actor involucrado, entityId afectado, y cambios de estado.

**RN-012: Umbral de Pre-aviso**  
El sistema envía el Mensaje 2 (pre-aviso) cuando la posición del ticket es ≤ 3, indicando que el cliente debe acercarse a la sucursal.

**RN-013: Estados de Asesor**  
Un asesor puede estar en uno de estos estados:
- AVAILABLE: disponible para recibir asignaciones
- BUSY: atendiendo un cliente (no recibe nuevas asignaciones)
- OFFLINE: no disponible (almuerzo, capacitación, etc.)

---

## 3. Enumeraciones

### 3.1 QueueType

Tipos de cola disponibles en el sistema:

| Valor | Display Name | Tiempo Promedio | Prioridad | Prefijo |
|-------|--------------|-----------------|-----------|---------|
| CAJA | Caja | 5 min | 1 | C |
| PERSONAL_BANKER | Personal Banker | 15 min | 2 | P |
| EMPRESAS | Empresas | 20 min | 3 | E |
| GERENCIA | Gerencia | 30 min | 4 | G |

### 3.2 TicketStatus

Estados posibles de un ticket:

| Valor | Descripción | Es Activo? |
|-------|-------------|------------|
| EN_ESPERA | Esperando asignación | Sí |
| PROXIMO | Próximo a ser atendido | Sí |
| ATENDIENDO | Siendo atendido | Sí |
| COMPLETADO | Atención finalizada | No |
| CANCELADO | Cancelado | No |
| NO_ATENDIDO | Cliente no se presentó | No |

### 3.3 AdvisorStatus

Estados posibles de un asesor:

| Valor | Descripción | Recibe Asignaciones? |
|-------|-------------|----------------------|
| AVAILABLE | Disponible | Sí |
| BUSY | Atendiendo cliente | No |
| OFFLINE | No disponible | No |

### 3.4 MessageTemplate

Plantillas de mensajes para Telegram:

| Valor | Descripción | Momento de Envío |
|-------|-------------|------------------|
| totem_ticket_creado | Confirmación de creación | Inmediato al crear ticket |
| totem_proximo_turno | Pre-aviso | Cuando posición ≤ 3 |
| totem_es_tu_turno | Turno activo | Al asignar a asesor |

---

## 4. Requerimientos Funcionales

### **RF-001: Crear Ticket Digital**

**Descripción:**  
El sistema debe permitir al cliente crear un ticket digital para ser atendido en sucursal, ingresando su identificación nacional (RUT/ID), número de teléfono y seleccionando el tipo de atención requerida. El sistema generará un número único de ticket, calculará la posición actual en cola y el tiempo estimado de espera basado en datos reales de la operación.

**Prioridad:** Alta

**Actor Principal:** Cliente

**Precondiciones:**
- Terminal de autoservicio disponible y funcional
- Sistema de gestión de colas operativo
- Conexión a base de datos activa

**Modelo de Datos (Campos del Ticket):**
- codigoReferencia: UUID único (ej: "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
- numero: String formato específico por cola (ej: "C01", "P15", "E03", "G02")
- nationalId: String, identificación nacional del cliente
- telefono: String, número de teléfono para Telegram
- branchOffice: String, nombre de la sucursal
- queueType: Enum (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)
- status: Enum (EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO)
- positionInQueue: Integer, posición actual en cola (calculada en tiempo real)
- estimatedWaitMinutes: Integer, minutos estimados de espera
- createdAt: Timestamp, fecha/hora de creación
- assignedAdvisor: Relación a entidad Advisor (null inicialmente)
- assignedModuleNumber: Integer 1-5 (null inicialmente)

**Reglas de Negocio Aplicables:**
- RN-001: Un cliente solo puede tener 1 ticket activo a la vez
- RN-005: Número de ticket formato: [Prefijo][Número secuencial 01-99]
- RN-006: Prefijos por cola: C=Caja, P=Personal Banker, E=Empresas, G=Gerencia
- RN-010: Cálculo de tiempo estimado: posiciónEnCola × tiempoPromedioCola

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Creación exitosa de ticket para cola de Caja**
```gherkin
Given el cliente con nationalId "12345678-9" no tiene tickets activos
And el terminal está en pantalla de selección de servicio
When el cliente ingresa:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | branchOffice | Sucursal Centro |
  | queueType    | CAJA            |
Then el sistema genera un ticket con:
  | Campo                 | Valor Esperado                    |
  | codigoReferencia      | UUID válido                       |
  | numero                | "C[01-99]"                        |
  | status                | EN_ESPERA                         |
  | positionInQueue       | Número > 0                        |
  | estimatedWaitMinutes  | positionInQueue × 5               |
  | assignedAdvisor       | null                              |
  | assignedModuleNumber  | null                              |
And el sistema retorna HTTP 201 Created
And el sistema programa el envío del mensaje "totem_ticket_creado" vía Telegram
And el sistema registra el evento "TICKET_CREATED" en auditoría
```

**Escenario 2: Rechazo por ticket activo existente**
```gherkin
Given el cliente con nationalId "12345678-9" tiene un ticket activo con status "EN_ESPERA"
When el cliente intenta crear un nuevo ticket con:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | branchOffice | Sucursal Centro |
  | queueType    | PERSONAL_BANKER |
Then el sistema retorna HTTP 409 Conflict
And el sistema retorna el mensaje "Cliente ya tiene un ticket activo"
And NO se crea un nuevo ticket
```

**Escenario 3: Validación de datos de entrada**
```gherkin
Given el terminal está en pantalla de selección de servicio
When el cliente ingresa datos inválidos:
  | Campo        | Valor Inválido |
  | nationalId   | ""             |
  | telefono     | "123"          |
  | branchOffice | null           |
  | queueType    | null           |
Then el sistema retorna HTTP 400 Bad Request
And el sistema muestra mensajes de validación específicos
And NO se crea el ticket
```

**Postcondiciones:**
- Ticket creado y almacenado en base de datos
- Mensaje de confirmación programado para envío vía Telegram
- Evento de auditoría registrado
- Cliente puede consultar su posición usando el UUID generado

**Excepciones:**
- **E001:** Cliente ya tiene ticket activo → HTTP 409 Conflict
- **E002:** Datos de entrada inválidos → HTTP 400 Bad Request
- **E003:** Error de sistema → HTTP 500 Internal Server Error

---

### **RF-002: Enviar Notificaciones Automáticas vía Telegram**

**Descripción:**  
El sistema debe enviar notificaciones automáticas al cliente vía Telegram en momentos clave del proceso de atención: confirmación de creación del ticket, pre-aviso cuando esté próximo a ser atendido, y notificación cuando sea su turno activo. Las notificaciones deben incluir información relevante como número de ticket, posición en cola, tiempo estimado y módulo de atención.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Cliente tiene número de teléfono válido registrado
- Bot de Telegram configurado y operativo
- Cliente ha iniciado conversación con el bot

**Modelo de Datos (Campos del Mensaje):**
- id: Long, identificador único del mensaje
- ticketId: Long, referencia al ticket
- phoneNumber: String, número de teléfono destino
- template: Enum (totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno)
- content: String, contenido del mensaje procesado
- status: Enum (PENDIENTE, ENVIADO, FALLIDO)
- attemptCount: Integer, número de intentos de envío
- sentAt: Timestamp, fecha/hora de envío exitoso
- createdAt: Timestamp, fecha/hora de creación

**Reglas de Negocio Aplicables:**
- RN-007: Reintentos automáticos hasta 3 veces
- RN-008: Backoff exponencial en reintentos
- RN-012: Pre-aviso cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Envío exitoso de confirmación de ticket**
```gherkin
Given existe un ticket con numero "C01" y telefono "+56912345678"
And el bot de Telegram está operativo
When el sistema programa el mensaje "totem_ticket_creado"
Then el sistema envía el mensaje:
  "✅ Ticket C01 creado. Posición #5, tiempo estimado: 25min. Sucursal Centro"
And el mensaje se marca como ENVIADO
And se registra el timestamp de envío
```

**Escenario 2: Envío de pre-aviso**
```gherkin
Given existe un ticket con numero "P03" en posición 3
And el ticket tiene status "EN_ESPERA"
When el sistema detecta que la posición es ≤ 3
Then el sistema envía el mensaje:
  "⏰ Pronto será tu turno P03. Acércate a la sucursal."
And el ticket cambia a status "PROXIMO"
```

**Escenario 3: Reintento automático por fallo**
```gherkin
Given un mensaje con attemptCount = 1 y status = PENDIENTE
And el envío a Telegram falla
When el sistema procesa reintentos
Then el sistema espera 30 segundos
And reintenta el envío
And incrementa attemptCount a 2
```

**Postcondiciones:**
- Mensaje enviado exitosamente o marcado como FALLIDO
- Cliente informado del estado de su ticket
- Registro de auditoría del envío

---

### **RF-003: Calcular Posición y Tiempo Estimado**

**Descripción:**  
El sistema debe calcular en tiempo real la posición actual del ticket en su cola específica y estimar el tiempo de espera basado en el tipo de servicio y la cantidad de tickets adelante. El cálculo debe actualizarse automáticamente cuando otros tickets son procesados o nuevos tickets son creados.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Ticket existe en estado activo (EN_ESPERA, PROXIMO, ATENDIENDO)
- Sistema de colas operativo

**Reglas de Negocio Aplicables:**
- RN-003: Orden FIFO dentro de cola
- RN-010: Cálculo de tiempo estimado

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Cálculo de posición en cola CAJA**
```gherkin
Given existen 4 tickets EN_ESPERA en cola CAJA creados antes del ticket "C05"
And el ticket "C05" tiene status "EN_ESPERA"
When el sistema calcula la posición
Then la posición del ticket "C05" es 5
And el tiempo estimado es 25 minutos (5 × 5min)
```

**Escenario 2: Actualización automática al completar ticket**
```gherkin
Given el ticket "C01" está en posición 1
And el ticket "C02" está en posición 2
When el ticket "C01" cambia a status "COMPLETADO"
Then la posición del ticket "C02" se actualiza a 1
And el tiempo estimado se recalcula a 5 minutos
```

---

### **RF-004: Asignar Ticket a Ejecutivo Automáticamente**

**Descripción:**  
El sistema debe asignar automáticamente tickets a ejecutivos disponibles siguiendo las reglas de prioridad de colas, orden FIFO y balanceo de carga. La asignación debe considerar la especialización del ejecutivo y su estado actual de disponibilidad.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Existe al menos un asesor con status AVAILABLE
- Existen tickets en estado EN_ESPERA

**Modelo de Datos (Campos del Advisor):**
- id: Long, identificador único
- name: String, nombre del asesor
- moduleNumber: Integer (1-5), número del módulo
- queueTypes: List<QueueType>, tipos de cola que puede atender
- status: Enum (AVAILABLE, BUSY, OFFLINE)
- assignedTicketsCount: Integer, cantidad de tickets asignados actualmente

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas
- RN-003: Orden FIFO dentro de cola
- RN-004: Balanceo de carga entre asesores

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Asignación por prioridad de cola**
```gherkin
Given existe un ticket "G01" en cola GERENCIA (prioridad 4)
And existe un ticket "C01" en cola CAJA (prioridad 1)
And ambos tickets tienen status "EN_ESPERA"
And existe un asesor AVAILABLE que atiende ambas colas
When el sistema ejecuta asignación automática
Then el ticket "G01" se asigna primero
And el asesor cambia a status "BUSY"
And el ticket cambia a status "ATENDIENDO"
```

**Escenario 2: Balanceo de carga**
```gherkin
Given existen 2 asesores AVAILABLE:
  | Asesor | assignedTicketsCount |
  | Ana    | 2                    |
  | Luis   | 1                    |
And existe un ticket "C01" EN_ESPERA
When el sistema asigna el ticket
Then el ticket se asigna a "Luis" (menor carga)
And assignedTicketsCount de Luis se incrementa a 2
```

---

### **RF-005: Completar Atención de Ticket**

**Descripción:**  
El sistema debe permitir al asesor marcar un ticket como completado una vez finalizada la atención al cliente. Al completar, el asesor debe quedar disponible para recibir nuevas asignaciones y el sistema debe actualizar las estadísticas operacionales.

**Prioridad:** Media

**Actor Principal:** Asesor

**Precondiciones:**
- Asesor tiene ticket asignado en estado ATENDIENDO
- Asesor está autenticado en el sistema

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Completar ticket exitosamente**
```gherkin
Given el asesor "Ana" tiene asignado el ticket "C01" con status "ATENDIENDO"
When el asesor marca el ticket como completado
Then el ticket cambia a status "COMPLETADO"
And el asesor cambia a status "AVAILABLE"
And assignedTicketsCount del asesor se decrementa en 1
And se registra el evento "TICKET_COMPLETED" en auditoría
```

---

### **RF-006: Consultar Estado del Ticket**

**Descripción:**  
El sistema debe permitir al cliente consultar el estado actual de su ticket usando el código de referencia (UUID), mostrando información actualizada sobre posición en cola, tiempo estimado de espera y estado actual.

**Prioridad:** Media

**Actor Principal:** Cliente

**Precondiciones:**
- Cliente tiene código de referencia válido
- Ticket existe en el sistema

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Consulta exitosa de ticket activo**
```gherkin
Given existe un ticket con UUID "abc-123" y numero "C01"
And el ticket tiene status "EN_ESPERA" y posición 3
When el cliente consulta con UUID "abc-123"
Then el sistema retorna:
  | Campo                | Valor     |
  | numero               | C01       |
  | status               | EN_ESPERA |
  | positionInQueue      | 3         |
  | estimatedWaitMinutes | 15        |
  | queueType            | CAJA      |
```

---

### **RF-007: Panel de Monitoreo para Supervisor**

**Descripción:**  
El sistema debe proporcionar un panel de monitoreo en tiempo real para supervisores, mostrando estadísticas operacionales, estado de colas, performance de asesores y métricas clave del servicio.

**Prioridad:** Media

**Actor Principal:** Supervisor

**Precondiciones:**
- Supervisor autenticado con permisos administrativos
- Sistema operativo con datos disponibles

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Visualizar dashboard completo**
```gherkin
Given el supervisor está autenticado
When accede al panel de monitoreo
Then el sistema muestra:
  - Total de tickets por estado
  - Tiempo promedio de atención por cola
  - Estado actual de todos los asesores
  - Tickets en espera por cola
  - Métricas de performance del día
```

---

### **RF-008: Registrar Auditoría de Eventos**

**Descripción:**  
El sistema debe registrar automáticamente todos los eventos críticos del proceso de atención en una tabla de auditoría para trazabilidad, análisis posterior y cumplimiento regulatorio.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Modelo de Datos (Campos de Auditoría):**
- id: Long, identificador único
- eventType: String, tipo de evento
- entityType: String, tipo de entidad afectada
- entityId: Long, ID de la entidad
- oldValue: String, valor anterior (JSON)
- newValue: String, valor nuevo (JSON)
- performedBy: String, usuario que ejecutó la acción
- timestamp: Timestamp, fecha/hora del evento

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Registro automático de creación de ticket**
```gherkin
Given se crea un nuevo ticket con ID 123
When el ticket se guarda en base de datos
Then el sistema registra en auditoría:
  | Campo       | Valor           |
  | eventType   | TICKET_CREATED  |
  | entityType  | TICKET          |
  | entityId    | 123             |
  | oldValue    | null            |
  | newValue    | {ticket_json}   |
  | performedBy | SYSTEM          |
```ull                              |
And el sistema retorna HTTP 201 Created
And el sistema programa el envío del mensaje "totem_ticket_creado" vía Telegram
And el sistema registra el evento "TICKET_CREATED" en auditoría
```

**Escenario 2: Rechazo por ticket activo existente**
```gherkin
Given el cliente con nationalId "12345678-9" tiene un ticket activo con status "EN_ESPERA"
When el cliente intenta crear un nuevo ticket con:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | branchOffice | Sucursal Centro |
  | queueType    | PERSONAL_BANKER |
Then el sistema retorna HTTP 409 Conflict
And el sistema retorna el mensaje "Cliente ya tiene un ticket activo"
And NO se crea un nuevo ticket
```

**Escenario 3: Validación de datos de entrada**
```gherkin
Given el terminal está en pantalla de selección de servicio
When el cliente ingresa datos inválidos:
  | Campo        | Valor Inválido |
  | nationalId   | ""             |
  | telefono     | "123"          |
  | branchOffice | null           |
  | queueType    | null           |
Then el sistema retorna HTTP 400 Bad Request
And el sistema muestra mensajes de validación específicos
And NO se crea el ticket
```

**Postcondiciones:**
- Ticket creado y almacenado en base de datos
- Mensaje de confirmación programado para envío vía Telegram
- Evento de auditoría registrado
- Cliente puede consultar su posición usando el UUID generado

**Excepciones:**
- **E001:** Cliente ya tiene ticket activo → HTTP 409 Conflict
- **E002:** Datos de entrada inválidos → HTTP 400 Bad Request
- **E003:** Error de sistema → HTTP 500 Internal Server Error

---ull                              |
And el sistema almacena el ticket en base de datos
And el sistema programa 3 mensajes de Telegram
And el sistema retorna HTTP 201 con JSON:
  {
    "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "C01",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "queueType": "CAJA"
  }
```

**Escenario 2: Error - Cliente ya tiene ticket activo**
```gherkin
Given el cliente con nationalId "12345678-9" tiene un ticket activo:
  | numero | status     | queueType      |
  | P05    | EN_ESPERA  | PERSONAL_BANKER|
When el cliente intenta crear un nuevo ticket con queueType CAJA
Then el sistema rechaza la creación
And el sistema retorna HTTP 409 Conflict con JSON:
  {
    "error": "TICKET_ACTIVO_EXISTENTE",
    "mensaje": "Ya tienes un ticket activo: P05",
    "ticketActivo": {
      "numero": "P05",
      "positionInQueue": 3,
      "estimatedWaitMinutes": 45
    }
  }
And el sistema NO crea un nuevo ticket
```

**Escenario 3: Validación - RUT/ID inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa nationalId vacío
Then el sistema retorna HTTP 400 Bad Request con JSON:
  {
    "error": "VALIDACION_FALLIDA",
    "campos": {
      "nationalId": "El RUT/ID es obligatorio"
    }
  }
And el sistema NO crea el ticket
```

**Escenario 4: Validación - Teléfono en formato inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa telefono "123"
Then el sistema retorna HTTP 400 Bad Request
And el mensaje de error especifica formato requerido "+56XXXXXXXXX"
```

**Escenario 5: Cálculo de posición - Primera persona en cola**
```gherkin
Given la cola de tipo PERSONAL_BANKER está vacía
When el cliente crea un ticket para PERSONAL_BANKER
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 15
And el número de ticket es "P01"
```

**Escenario 6: Cálculo de posición - Cola con tickets existentes**
```gherkin
Given la cola de tipo EMPRESAS tiene 4 tickets EN_ESPERA
When el cliente crea un nuevo ticket para EMPRESAS
Then el sistema calcula positionInQueue = 5
And estimatedWaitMinutes = 100
And el cálculo es: 5 × 20min = 100min
```

**Escenario 7: Creación sin teléfono (cliente no quiere notificaciones)**
```gherkin
Given el cliente no proporciona número de teléfono
When el cliente crea un ticket
Then el sistema crea el ticket exitosamente
And el sistema NO programa mensajes de Telegram
```

**Postcondiciones:**
- Ticket almacenado en base de datos con estado EN_ESPERA
- 3 mensajes programados (si hay teléfono)
- Evento de auditoría registrado: "TICKET_CREADO"

**Endpoints HTTP:**
- POST /api/tickets - Crear nuevo ticket

---

### **RF-002: Enviar Notificaciones Automáticas vía Telegram**

**Descripción:**  
El sistema debe enviar automáticamente tres tipos de mensajes vía Telegram Bot a los clientes que proporcionaron su número de teléfono al crear el ticket. Los mensajes incluyen confirmación de creación, pre-aviso cuando están próximos a ser atendidos, y notificación cuando es su turno activo con información del módulo y asesor asignado.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Ticket creado con teléfono válido
- Telegram Bot configurado y activo
- Cliente tiene cuenta de Telegram
- Scheduler de mensajes operativo

**Modelo de Datos (Entidad Mensaje):**
- id: BIGSERIAL (primary key)
- ticket_id: BIGINT (foreign key a ticket)
- plantilla: String (totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno)
- estadoEnvio: Enum (PENDIENTE, ENVIADO, FALLIDO)
- fechaProgramada: Timestamp
- fechaEnvio: Timestamp (nullable)
- telegramMessageId: String (nullable, retornado por Telegram API)
- intentos: Integer (contador de reintentos, default 0)

**Plantillas de Mensajes:**

**1. totem_ticket_creado:**
```
✅ <b>Ticket Creado</b>
Tu número de turno: <b>{numero}</b>
📍 Posición en cola: <b>#{posicion}</b>
⏰ Tiempo estimado: <b>{tiempo} minutos</b>

Te notificaremos cuando estés próximo.
```

**2. totem_proximo_turno:**
```
🔔 <b>¡Pronto será tu turno!</b>
Turno: <b>{numero}</b>
Faltan aproximadamente 3 turnos.

📍 Por favor, acércate a la sucursal.
```

**3. totem_es_tu_turno:**
```
🚨 <b>¡ES TU TURNO {numero}!</b>
📍 Dirígete al módulo: <b>{modulo}</b>
👤 Asesor: <b>{nombreAsesor}</b>
```

**Reglas de Negocio Aplicables:**
- RN-007: 3 reintentos automáticos para mensajes fallidos
- RN-008: Backoff exponencial (30s, 60s, 120s)
- RN-011: Auditoría obligatoria de envíos
- RN-012: Mensaje 2 cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Envío exitoso del Mensaje 1 (confirmación)**
```gherkin
Given un ticket fue creado con telefono "+56912345678"
And el Telegram Bot está configurado correctamente
When el sistema programa el Mensaje 1 (totem_ticket_creado)
Then el sistema crea un registro en tabla Mensaje con:
  | Campo           | Valor                |
  | plantilla       | totem_ticket_creado  |
  | estadoEnvio     | PENDIENTE           |
  | intentos        | 0                   |
And el sistema envía el mensaje vía Telegram API
And Telegram API retorna message_id "12345"
Then el sistema actualiza el registro:
  | Campo              | Valor    |
  | estadoEnvio        | ENVIADO  |
  | telegramMessageId  | 12345    |
  | fechaEnvio         | now()    |
And el sistema registra auditoría: "MENSAJE_ENVIADO"
```

**Escenario 2: Envío exitoso del Mensaje 2 (pre-aviso)**
```gherkin
Given un ticket tiene positionInQueue = 3
And el ticket tiene telefono válido
When el sistema detecta que posición ≤ 3
Then el sistema programa Mensaje 2 (totem_proximo_turno)
And el mensaje contiene el texto: "Faltan aproximadamente 3 turnos"
And el sistema envía el mensaje exitosamente
Then estadoEnvio = ENVIADO
```

**Escenario 3: Envío exitoso del Mensaje 3 (turno activo)**
```gherkin
Given un ticket fue asignado al asesor "Juan Pérez" en módulo 3
And el ticket tiene telefono "+56987654321"
When el sistema programa Mensaje 3 (totem_es_tu_turno)
Then el mensaje contiene:
  | Variable      | Valor       |
  | {numero}      | C05         |
  | {modulo}      | 3           |
  | {nombreAsesor}| Juan Pérez  |
And el sistema envía el mensaje exitosamente
```

**Escenario 4: Fallo de red en primer intento, éxito en segundo**
```gherkin
Given un mensaje está programado para envío
And el primer intento falla por timeout de red
When el sistema ejecuta el primer reintento después de 30 segundos
Then el sistema incrementa intentos = 1
And el segundo intento es exitoso
Then estadoEnvio = ENVIADO
And intentos = 1
```

**Escenario 5: Tres reintentos fallidos → estado FALLIDO**
```gherkin
Given un mensaje está en estado PENDIENTE
When el intento inicial falla
And el reintento 1 (30s después) falla
And el reintento 2 (60s después) falla  
And el reintento 3 (120s después) falla
Then el sistema marca estadoEnvio = FALLIDO
And intentos = 3
And el sistema registra auditoría: "MENSAJE_FALLIDO"
And el sistema NO programa más reintentos
```

**Escenario 6: Backoff exponencial entre reintentos**
```gherkin
Given un mensaje falló en el primer intento a las 10:00:00
When el sistema programa el primer reintento
Then el reintento se ejecuta a las 10:00:30 (30s después)
And si falla, el segundo reintento se ejecuta a las 10:01:30 (60s después)
And si falla, el tercer reintento se ejecuta a las 10:03:30 (120s después)
```

**Escenario 7: Cliente sin teléfono, no se programan mensajes**
```gherkin
Given un cliente crea un ticket sin proporcionar teléfono
When el sistema procesa la creación del ticket
Then el sistema NO crea registros en tabla Mensaje
And el sistema NO programa ningún envío
And el ticket se crea exitosamente sin notificaciones
```

**Postcondiciones:**
- Mensaje insertado en BD con estado según resultado
- telegram_message_id almacenado si éxito
- Intentos incrementado en cada reintento
- Auditoría registrada para cada envío/fallo

**Endpoints HTTP:**
- Ninguno (proceso interno automatizado por scheduler)

---

### **RF-003: Calcular Posición y Tiempo Estimado**

**Descripción:**  
El sistema debe calcular en tiempo real la posición exacta del cliente en cola y estimar el tiempo de espera basado en la posición actual, tiempo promedio de atención por tipo de cola, y cantidad de asesores disponibles. El cálculo debe actualizarse automáticamente cuando otros tickets cambien de estado o se asignen a asesores.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Ticket existe en estado EN_ESPERA o PROXIMO
- Base de datos con información actualizada de colas
- Configuración de tiempos promedio por cola

**Algoritmos de Cálculo:**

**1. Cálculo de Posición:**
```
posición = COUNT(tickets EN_ESPERA con createdAt < ticket.createdAt 
             AND queueType = ticket.queueType) + 1
```

**2. Cálculo de Tiempo Estimado:**
```
tiempoEstimado = posición × tiempoPromedioCola
```

**Tiempos Promedio por Cola:**
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos  
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos

**Reglas de Negocio Aplicables:**
- RN-003: Orden FIFO dentro de cola (createdAt determina orden)
- RN-010: Fórmula de cálculo: posiciónEnCola × tiempoPromedioCola
- RN-012: Cambio a estado PROXIMO cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Cálculo de posición para primer ticket en cola**
```gherkin
Given la cola CAJA está vacía
When se crea un nuevo ticket para CAJA
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 5
And el cálculo es: 1 × 5min = 5min
```

**Escenario 2: Cálculo con múltiples tickets en cola**
```gherkin
Given la cola PERSONAL_BANKER tiene 3 tickets EN_ESPERA:
  | numero | createdAt           |
  | P01    | 2025-01-15 10:00:00 |
  | P02    | 2025-01-15 10:05:00 |
  | P03    | 2025-01-15 10:10:00 |
When se crea un nuevo ticket P04 a las 10:15:00
Then el sistema calcula positionInQueue = 4
And estimatedWaitMinutes = 60
And el cálculo es: 4 × 15min = 60min
```

**Escenario 3: Recalculo automático cuando ticket anterior se asigna**
```gherkin
Given el ticket P05 tiene positionInQueue = 3 y estimatedWaitMinutes = 45
And hay 2 tickets antes en la cola
When el primer ticket (P01) cambia a estado ATENDIENDO
Then el sistema recalcula automáticamente:
  | Campo                | Valor Anterior | Valor Nuevo |
  | positionInQueue      | 3             | 2           |
  | estimatedWaitMinutes | 45            | 30          |
And el cálculo es: 2 × 15min = 30min
```

**Escenario 4: Cambio a estado PROXIMO cuando posición ≤ 3**
```gherkin
Given el ticket E07 tiene positionInQueue = 4 y status = EN_ESPERA
When otros tickets se procesan y E07 queda en posición 3
Then el sistema actualiza:
  | Campo           | Valor Anterior | Valor Nuevo |
  | positionInQueue | 4             | 3           |
  | status          | EN_ESPERA     | PROXIMO     |
And el sistema programa Mensaje 2 (pre-aviso)
```

**Escenario 5: Consulta de posición vía API**
```gherkin
Given el ticket G02 existe con positionInQueue = 2
When el cliente consulta GET /api/tickets/G02/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "G02",
    "positionInQueue": 2,
    "estimatedWaitMinutes": 60,
    "status": "PROXIMO",
    "queueType": "GERENCIA",
    "calculatedAt": "2025-01-15T10:30:00Z"
  }
```

**Escenario 6: Cálculo para diferentes tipos de cola simultáneamente**
```gherkin
Given existen tickets en múltiples colas:
  | Cola            | Tickets EN_ESPERA | Tiempo Promedio |
  | CAJA           | 5                 | 5 min           |
  | PERSONAL_BANKER | 3                 | 15 min          |
  | EMPRESAS       | 2                 | 20 min          |
When se crean nuevos tickets simultáneamente
Then cada cola calcula independientemente:
  | Cola            | Nueva Posición | Tiempo Estimado |
  | CAJA           | 6                | 30 min          |
  | PERSONAL_BANKER | 4                | 60 min          |
  | EMPRESAS       | 3                | 60 min          |
```

**Postcondiciones:**
- Posición actualizada en tiempo real
- Tiempo estimado recalculado
- Estado cambiado a PROXIMO si posición ≤ 3
- Mensaje 2 programado si aplica
- Auditoría registrada para cambios de estado

**Endpoints HTTP:**
- GET /api/tickets/{numero}/position - Consultar posición actual

---

### **RF-004: Asignar Ticket a Ejecutivo Automáticamente**

**Descripción:**  
El sistema debe asignar automáticamente el siguiente ticket en cola cuando un ejecutivo se libere, considerando la prioridad de colas, balanceo de carga entre ejecutivos disponibles, y orden FIFO dentro de cada cola. La asignación debe ser inmediata y notificar tanto al cliente como al ejecutivo.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Al menos un asesor en estado AVAILABLE
- Tickets en estado EN_ESPERA o PROXIMO en alguna cola
- Sistema de asignación operativo

**Modelo de Datos (Entidad Advisor):**
- id: BIGSERIAL (primary key)
- name: String, nombre completo del asesor
- email: String, correo electrónico corporativo
- status: Enum (AVAILABLE, BUSY, OFFLINE)
- moduleNumber: Integer 1-5, número de módulo asignado
- assignedTicketsCount: Integer, contador de tickets asignados actualmente
- queueTypes: Array, tipos de cola que puede atender
- lastAssignedAt: Timestamp, última asignación recibida

**Algoritmo de Asignación:**

**1. Selección de Cola (por prioridad):**
```
FOR cada cola en orden de prioridad (GERENCIA=4, EMPRESAS=3, PERSONAL_BANKER=2, CAJA=1):
  IF cola tiene tickets EN_ESPERA o PROXIMO:
    RETURN primer ticket de esa cola (orden FIFO por createdAt)
```

**2. Selección de Asesor (balanceo de carga):**
```
FILTER asesores WHERE status = AVAILABLE 
                AND queueTypes CONTAINS ticket.queueType
ORDER BY assignedTicketsCount ASC, lastAssignedAt ASC
RETURN primer asesor
```

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
- RN-003: Orden FIFO dentro de cada cola
- RN-004: Balanceo de carga (menor assignedTicketsCount)
- RN-011: Auditoría obligatoria de asignaciones
- RN-013: Solo asesores AVAILABLE reciben asignaciones

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Asignación exitosa con un asesor disponible**
```gherkin
Given existe un ticket P05 en estado EN_ESPERA desde las 10:00:00
And existe un asesor disponible:
  | Campo               | Valor           |
  | name                | Juan Pérez      |
  | status              | AVAILABLE       |
  | moduleNumber        | 3               |
  | assignedTicketsCount| 0               |
  | queueTypes          | [PERSONAL_BANKER]|
When el sistema ejecuta el proceso de asignación
Then el sistema asigna P05 a Juan Pérez:
  | Campo Ticket        | Valor Anterior | Valor Nuevo |
  | status              | EN_ESPERA     | ATENDIENDO  |
  | assignedAdvisor     | null          | Juan Pérez  |
  | assignedModuleNumber| null          | 3           |
And el sistema actualiza el asesor:
  | Campo Asesor        | Valor Anterior | Valor Nuevo |
  | status              | AVAILABLE     | BUSY        |
  | assignedTicketsCount| 0             | 1           |
  | lastAssignedAt      | null          | now()       |
And el sistema programa Mensaje 3 (totem_es_tu_turno)
And el sistema registra auditoría: "TICKET_ASIGNADO"
```

**Escenario 2: Prioridad de colas - GERENCIA antes que CAJA**
```gherkin
Given existen tickets en múltiples colas:
  | numero | queueType       | createdAt           | status    |
  | C10    | CAJA           | 2025-01-15 09:00:00 | EN_ESPERA |
  | G01    | GERENCIA       | 2025-01-15 09:30:00 | EN_ESPERA |
And existe un asesor AVAILABLE que puede atender ambas colas
When el sistema ejecuta asignación
Then el sistema asigna G01 (GERENCIA) antes que C10 (CAJA)
And C10 permanece EN_ESPERA
```

**Escenario 3: Balanceo de carga entre múltiples asesores**
```gherkin
Given existen 3 asesores AVAILABLE para CAJA:
  | name        | assignedTicketsCount | lastAssignedAt      |
  | Ana García  | 2                   | 2025-01-15 09:00:00 |
  | Luis Torres | 1                   | 2025-01-15 09:15:00 |
  | María Silva| 1                   | 2025-01-15 09:10:00 |
And existe un ticket C15 EN_ESPERA
When el sistema ejecuta asignación
Then el sistema selecciona María Silva (menor assignedTicketsCount + lastAssignedAt más antiguo)
And María Silva.assignedTicketsCount se incrementa a 2
```

**Escenario 4: Orden FIFO dentro de la misma cola**
```gherkin
Given la cola EMPRESAS tiene 3 tickets:
  | numero | createdAt           | status    |
  | E05    | 2025-01-15 10:00:00 | EN_ESPERA |
  | E06    | 2025-01-15 10:05:00 | EN_ESPERA |
  | E07    | 2025-01-15 10:10:00 | EN_ESPERA |
And existe un asesor AVAILABLE para EMPRESAS
When el sistema ejecuta asignación
Then el sistema asigna E05 (createdAt más antiguo)
And E06 y E07 permanecen EN_ESPERA
```

**Escenario 5: No hay asesores disponibles**
```gherkin
Given existen tickets EN_ESPERA en todas las colas
And todos los asesores están en estado BUSY u OFFLINE
When el sistema ejecuta asignación
Then el sistema NO asigna ningún ticket
And todos los tickets permanecen EN_ESPERA
And el sistema registra evento: "NO_ASESORES_DISPONIBLES"
```

**Escenario 6: Asesor no puede atender tipo de cola**
```gherkin
Given existe un ticket G03 de tipo GERENCIA EN_ESPERA
And existe un asesor AVAILABLE:
  | name       | queueTypes        |
  | Pedro Ruiz | [CAJA, PERSONAL_BANKER] |
When el sistema ejecuta asignación
Then el sistema NO asigna G03 a Pedro Ruiz
And G03 permanece EN_ESPERA
And el sistema busca otros asesores que puedan atender GERENCIA
```

**Escenario 7: Liberación de asesor desencadena nueva asignación**
```gherkin
Given un asesor cambia de BUSY a AVAILABLE
And existen tickets EN_ESPERA en cola
When el sistema detecta el cambio de estado del asesor
Then el sistema ejecuta automáticamente el proceso de asignación
And asigna el siguiente ticket según prioridad y FIFO
```

**Postcondiciones:**
- Ticket cambiado a estado ATENDIENDO
- Asesor cambiado a estado BUSY
- assignedTicketsCount incrementado
- Mensaje 3 programado para el cliente
- Auditoría registrada
- Otros tickets recalculan posición automáticamente

**Endpoints HTTP:**
- Ninguno (proceso interno automatizado)

---

### **RF-005: Gestionar Múltiples Colas**

**Descripción:**  
El sistema debe gestionar simultáneamente cuatro tipos de cola con diferentes características operacionales, tiempos promedio de atención y prioridades. Cada cola opera independientemente pero comparte el pool de asesores disponibles según sus competencias y configuración.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Sistema de colas inicializado
- Configuración de tipos de cola cargada
- Asesores configurados con queueTypes permitidos

**Configuración de Colas:**

| Cola | Tiempo Promedio | Prioridad | Prefijo | Descripción |
|------|-----------------|-----------|---------|-------------|
| CAJA | 5 min | 1 (baja) | C | Transacciones básicas, depósitos, retiros |
| PERSONAL_BANKER | 15 min | 2 (media) | P | Productos financieros, créditos, inversiones |
| EMPRESAS | 20 min | 3 (media-alta) | E | Clientes corporativos, servicios empresariales |
| GERENCIA | 30 min | 4 (máxima) | G | Casos especiales, reclamos, autorizaciones |

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas para asignación automática
- RN-003: Orden FIFO dentro de cada cola independientemente
- RN-005: Formato de número con prefijo específico por cola
- RN-006: Prefijos únicos por tipo de cola
- RN-010: Tiempo estimado basado en características de cada cola

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Operación simultánea de las 4 colas**
```gherkin
Given el sistema está operativo
When se crean tickets simultáneamente en las 4 colas:
  | Cola            | Tickets Creados | Prefijos Esperados |
  | CAJA           | 3              | C01, C02, C03      |
  | PERSONAL_BANKER | 2              | P01, P02           |
  | EMPRESAS       | 1              | E01                |
  | GERENCIA       | 1              | G01                |
Then cada cola opera independientemente:
  | Cola            | Tickets EN_ESPERA | Tiempo Estimado Promedio |
  | CAJA           | 3                | 15 min (3×5)           |
  | PERSONAL_BANKER | 2                | 30 min (2×15)          |
  | EMPRESAS       | 1                | 20 min (1×20)          |
  | GERENCIA       | 1                | 30 min (1×30)          |
```

**Escenario 2: Consulta de estado de cola específica**
```gherkin
Given la cola EMPRESAS tiene 4 tickets:
  | numero | status     | positionInQueue |
  | E01    | ATENDIENDO | 0              |
  | E02    | EN_ESPERA  | 1              |
  | E03    | EN_ESPERA  | 2              |
  | E04    | PROXIMO    | 3              |
When se consulta GET /api/admin/queues/EMPRESAS
Then el sistema retorna HTTP 200 con JSON:
  {
    "queueType": "EMPRESAS",
    "totalTickets": 4,
    "ticketsEnEspera": 2,
    "ticketsAtendiendo": 1,
    "ticketsProximos": 1,
    "tiempoPromedioAtencion": 20,
    "tiempoEstimadoEspera": 40
  }
```

**Escenario 3: Estadísticas de cola con métricas operacionales**
```gherkin
Given la cola CAJA ha procesado tickets durante el día
When se consulta GET /api/admin/queues/CAJA/stats
Then el sistema retorna estadísticas:
  {
    "queueType": "CAJA",
    "fecha": "2025-01-15",
    "ticketsCreados": 45,
    "ticketsCompletados": 42,
    "ticketsCancelados": 2,
    "ticketsNoAtendidos": 1,
    "tiempoPromedioReal": 4.8,
    "tiempoEsperaPromedio": 12.5,
    "eficiencia": 93.3
  }
```

**Escenario 4: Asignación respetando prioridades entre colas**
```gherkin
Given existen tickets pendientes en múltiples colas:
  | Cola            | Tickets EN_ESPERA | Prioridad |
  | CAJA           | 5                | 1         |
  | PERSONAL_BANKER | 3                | 2         |
  | EMPRESAS       | 2                | 3         |
  | GERENCIA       | 1                | 4         |
And un asesor se libera y puede atender todas las colas
When el sistema ejecuta asignación automática
Then el sistema asigna el ticket de GERENCIA (prioridad 4)
And los tickets de otras colas permanecen EN_ESPERA
```

**Escenario 5: Reinicio diario de numeración por cola**
```gherkin
Given es el final del día y existen tickets:
  | Cola            | Último Número |
  | CAJA           | C47           |
  | PERSONAL_BANKER | P23           |
  | EMPRESAS       | E15           |
  | GERENCIA       | G08           |
When el sistema ejecuta el proceso de reinicio diario
Then al día siguiente los primeros tickets son:
  | Cola            | Primer Número |
  | CAJA           | C01           |
  | PERSONAL_BANKER | P01           |
  | EMPRESAS       | E01           |
  | GERENCIA       | G01           |
```

**Postcondiciones:**
- Cada cola mantiene su numeración independiente
- Estadísticas actualizadas por cola
- Prioridades respetadas en asignaciones
- Métricas operacionales disponibles
- Auditoría por cola registrada

**Endpoints HTTP:**
- GET /api/admin/queues/{type} - Consultar estado de cola específica
- GET /api/admin/queues/{type}/stats - Estadísticas operacionales de cola

---

### **RF-006: Consultar Estado del Ticket**

**Descripción:**  
El sistema debe permitir al cliente consultar en cualquier momento el estado actual de su ticket, mostrando información actualizada sobre posición en cola, tiempo estimado de espera, estado actual, y ejecutivo asignado si aplica. La consulta puede realizarse por UUID o por número de ticket.

**Prioridad:** Media

**Actor Principal:** Cliente

**Precondiciones:**
- Ticket existe en el sistema
- Conexión a base de datos activa
- API de consultas operativa

**Información Retornada:**
- Número de ticket
- Estado actual (EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, etc.)
- Posición actual en cola (si aplica)
- Tiempo estimado de espera actualizado
- Ejecutivo asignado y módulo (si está asignado)
- Timestamp de última actualización
- Tipo de cola

**Reglas de Negocio Aplicables:**
- RN-009: Estados válidos de ticket
- RN-010: Cálculo de tiempo estimado en tiempo real
- RN-012: Estado PROXIMO cuando posición ≤ 3

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Consulta exitosa de ticket EN_ESPERA**
```gherkin
Given existe un ticket con los siguientes datos:
  | Campo               | Valor                    |
  | codigoReferencia    | abc123-def456-ghi789     |
  | numero              | P07                      |
  | status              | EN_ESPERA               |
  | positionInQueue     | 5                       |
  | estimatedWaitMinutes| 75                      |
  | queueType           | PERSONAL_BANKER         |
  | assignedAdvisor     | null                    |
When el cliente consulta GET /api/tickets/abc123-def456-ghi789
Then el sistema retorna HTTP 200 con JSON:
  {
    "codigoReferencia": "abc123-def456-ghi789",
    "numero": "P07",
    "status": "EN_ESPERA",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 75,
    "queueType": "PERSONAL_BANKER",
    "assignedAdvisor": null,
    "assignedModuleNumber": null,
    "lastUpdated": "2025-01-15T14:30:00Z",
    "branchOffice": "Sucursal Centro"
  }
```

**Escenario 2: Consulta de ticket ATENDIENDO con asesor asignado**
```gherkin
Given existe un ticket asignado:
  | Campo               | Valor           |
  | numero              | G02             |
  | status              | ATENDIENDO      |
  | assignedAdvisor     | Ana García      |
  | assignedModuleNumber| 4               |
  | positionInQueue     | 0               |
When el cliente consulta GET /api/tickets/G02/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "G02",
    "status": "ATENDIENDO",
    "positionInQueue": 0,
    "estimatedWaitMinutes": 0,
    "assignedAdvisor": {
      "name": "Ana García",
      "moduleNumber": 4
    },
    "queueType": "GERENCIA",
    "message": "Dirígete al módulo 4 - Ana García te está esperando"
  }
```

**Escenario 3: Consulta de ticket PROXIMO (pre-aviso)**
```gherkin
Given un ticket tiene posición 2 en cola EMPRESAS
When el cliente consulta su estado
Then el sistema retorna:
  {
    "numero": "E05",
    "status": "PROXIMO",
    "positionInQueue": 2,
    "estimatedWaitMinutes": 40,
    "queueType": "EMPRESAS",
    "message": "Pronto será tu turno. Acércate a la sucursal.",
    "assignedAdvisor": null
  }
```

**Escenario 4: Consulta de ticket COMPLETADO**
```gherkin
Given un ticket fue completado hace 30 minutos
When el cliente consulta GET /api/tickets/C15/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "C15",
    "status": "COMPLETADO",
    "positionInQueue": null,
    "estimatedWaitMinutes": null,
    "completedAt": "2025-01-15T13:45:00Z",
    "attendedBy": "Luis Torres",
    "moduleNumber": 2,
    "message": "Tu atención ha sido completada. Gracias por tu visita."
  }
```

**Escenario 5: Ticket no existe**
```gherkin
Given no existe un ticket con UUID "invalid-uuid-123"
When el cliente consulta GET /api/tickets/invalid-uuid-123
Then el sistema retorna HTTP 404 Not Found con JSON:
  {
    "error": "TICKET_NO_ENCONTRADO",
    "mensaje": "El ticket solicitado no existe o ha expirado",
    "codigo": "invalid-uuid-123"
  }
```

**Escenario 6: Actualización en tiempo real de posición**
```gherkin
Given un ticket P10 tiene posición 4 a las 14:00:00
And otro ticket P08 cambia a ATENDIENDO a las 14:05:00
When el cliente consulta P10 a las 14:06:00
Then el sistema retorna posición actualizada:
  {
    "numero": "P10",
    "positionInQueue": 3,
    "estimatedWaitMinutes": 45,
    "lastUpdated": "2025-01-15T14:05:30Z",
    "message": "Tu posición ha mejorado. Tiempo estimado actualizado."
  }
```

**Postcondiciones:**
- Información actualizada retornada al cliente
- Cálculos de tiempo realizados en tiempo real
- Estado actual reflejado correctamente
- Mensajes contextuales según estado

**Endpoints HTTP:**
- GET /api/tickets/{codigoReferencia} - Consultar por UUID
- GET /api/tickets/{numero}/position - Consultar por número de ticket

---

### **RF-007: Panel de Monitoreo para Supervisor**

**Descripción:**  
El sistema debe proveer un dashboard en tiempo real para supervisores que muestre métricas operacionales, estado de colas, asesores disponibles, tiempos promedio de atención, y alertas de situaciones críticas. El panel debe actualizarse automáticamente cada 5 segundos para mantener información actualizada.

**Prioridad:** Media

**Actor Principal:** Supervisor

**Precondiciones:**
- Usuario autenticado con rol de supervisor
- Sistema operativo con datos en tiempo real
- Dashboard web accesible

**Componentes del Dashboard:**

**1. Resumen General:**
- Total de tickets activos
- Tickets completados hoy
- Tiempo promedio de espera actual
- Eficiencia operacional (%)

**2. Estado de Colas:**
- Tickets en espera por cola
- Tiempo estimado por cola
- Cola con mayor congestión

**3. Estado de Asesores:**
- Asesores disponibles/ocupados/offline
- Carga de trabajo por asesor
- Tiempo promedio de atención por asesor

**4. Alertas Críticas:**
- Colas con más de 15 personas esperando
- Tiempos de espera superiores a 60 minutos
- Asesores offline por más de 30 minutos
- Fallos en envío de mensajes

**Reglas de Negocio Aplicables:**
- RN-011: Auditoría de accesos al dashboard
- RN-013: Estados de asesores reflejados en tiempo real

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Dashboard muestra resumen general actualizado**
```gherkin
Given el supervisor accede al dashboard a las 14:30:00
And existen los siguientes datos en el sistema:
  | Métrica              | Valor |
  | Tickets activos     | 23    |
  | Tickets completados | 87    |
  | Tiempo promedio     | 18min |
When el dashboard se carga
Then el sistema muestra el resumen:
  {
    "ticketsActivos": 23,
    "ticketsCompletadosHoy": 87,
    "tiempoPromedioEspera": 18,
    "eficienciaOperacional": 92.5,
    "ultimaActualizacion": "2025-01-15T14:30:00Z"
  }
And el dashboard se actualiza automáticamente cada 5 segundos
```

**Escenario 2: Estado de colas con información detallada**
```gherkin
Given existen tickets distribuidos en las colas:
  | Cola            | EN_ESPERA | ATENDIENDO | Tiempo Estimado |
  | CAJA           | 8         | 2          | 40 min          |
  | PERSONAL_BANKER | 5         | 1          | 75 min          |
  | EMPRESAS       | 3         | 1          | 60 min          |
  | GERENCIA       | 2         | 0          | 60 min          |
When el supervisor consulta GET /api/admin/dashboard
Then el sistema retorna el estado de colas:
  {
    "colas": [
      {
        "tipo": "CAJA",
        "enEspera": 8,
        "atendiendo": 2,
        "tiempoEstimado": 40,
        "estado": "NORMAL"
      },
      {
        "tipo": "PERSONAL_BANKER",
        "enEspera": 5,
        "atendiendo": 1,
        "tiempoEstimado": 75,
        "estado": "ALERTA"
      }
    ]
  }
```

**Escenario 3: Estado de asesores con carga de trabajo**
```gherkin
Given existen 5 asesores configurados:
  | Nombre      | Estado    | Módulo | Tickets Asignados | Tiempo Promedio |
  | Ana García  | BUSY      | 1      | 1                | 12 min         |
  | Luis Torres | AVAILABLE | 2      | 0                | 15 min         |
  | María Silva| BUSY      | 3      | 1                | 18 min         |
  | Pedro Ruiz  | OFFLINE   | 4      | 0                | N/A            |
  | Juan Pérez  | AVAILABLE | 5      | 0                | 14 min         |
When el supervisor consulta GET /api/admin/advisors
Then el sistema retorna:
  {
    "totalAsesores": 5,
    "disponibles": 2,
    "ocupados": 2,
    "offline": 1,
    "asesores": [
      {
        "name": "Ana García",
        "status": "BUSY",
        "moduleNumber": 1,
        "assignedTicketsCount": 1,
        "averageServiceTime": 12
      }
    ]
  }
```

**Escenario 4: Alertas críticas generadas automáticamente**
```gherkin
Given la cola EMPRESAS tiene 16 tickets EN_ESPERA
And el tiempo de espera promedio es 85 minutos
When el sistema evalúa las condiciones de alerta
Then el sistema genera alertas:
  {
    "alertas": [
      {
        "tipo": "COLA_CONGESTIONADA",
        "cola": "EMPRESAS",
        "ticketsEnEspera": 16,
        "umbral": 15,
        "severidad": "ALTA",
        "timestamp": "2025-01-15T14:35:00Z"
      },
      {
        "tipo": "TIEMPO_ESPERA_EXCESIVO",
        "cola": "EMPRESAS",
        "tiempoActual": 85,
        "umbral": 60,
        "severidad": "MEDIA",
        "timestamp": "2025-01-15T14:35:00Z"
      }
    ]
  }
And las alertas se muestran prominentemente en el dashboard
```

**Escenario 5: Actualización automática cada 5 segundos**
```gherkin
Given el supervisor tiene el dashboard abierto
And los datos cambian en el sistema:
  | Tiempo | Evento                    | Impacto                |
  | 14:30:00 | Ticket C15 completado   | Tickets activos: 22    |
  | 14:30:03 | Asesor cambia a OFFLINE | Disponibles: 1         |
When transcurren 5 segundos desde la última actualización
Then el dashboard se actualiza automáticamente
And muestra los nuevos valores sin recargar la página
And el timestamp de última actualización se actualiza
```

**Escenario 6: Estadísticas operacionales del día**
```gherkin
Given es las 16:00:00 y se han procesado tickets durante el día
When el supervisor consulta GET /api/admin/summary
Then el sistema retorna estadísticas del día:
  {
    "fecha": "2025-01-15",
    "ticketsCreados": 156,
    "ticketsCompletados": 142,
    "ticketsCancelados": 8,
    "ticketsNoAtendidos": 3,
    "eficiencia": 91.0,
    "tiempoPromedioEspera": 16.5,
    "tiempoPromedioAtencion": 12.8,
    "horasPico": ["10:00-11:00", "14:00-15:00"],
    "colaMasCongestionada": "PERSONAL_BANKER"
  }
```

**Postcondiciones:**
- Dashboard actualizado con información en tiempo real
- Alertas críticas visibles para acción inmediata
- Métricas operacionales disponibles para análisis
- Acceso registrado en auditoría

**Endpoints HTTP:**
- GET /api/admin/dashboard - Dashboard principal con resumen
- GET /api/admin/summary - Estadísticas operacionales del día
- GET /api/admin/advisors - Estado actual de asesores
- GET /api/admin/advisors/stats - Estadísticas de rendimiento de asesores
- PUT /api/admin/advisors/{id}/status - Cambiar estado de asesor

---

### **RF-008: Registrar Auditoría de Eventos**

**Descripción:**  
El sistema debe registrar automáticamente todos los eventos críticos del sistema para trazabilidad, cumplimiento normativo y análisis operacional. Cada evento debe incluir timestamp, tipo de evento, actor involucrado, entidad afectada, y cambios de estado para permitir reconstrucción completa del flujo operacional.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Sistema de auditoría configurado
- Base de datos de auditoría disponible
- Eventos del sistema operativos

**Modelo de Datos (Entidad AuditLog):**
- id: BIGSERIAL (primary key)
- timestamp: Timestamp, fecha/hora exacta del evento
- tipoEvento: String, tipo de evento registrado
- actor: String, usuario o sistema que ejecutó la acción
- entityType: String, tipo de entidad afectada (Ticket, Advisor, Message)
- entityId: String, identificador de la entidad afectada
- cambiosEstado: JSON, estado anterior y nuevo (si aplica)
- detallesAdicionales: JSON, información contextual del evento
- ipAddress: String, dirección IP de origen (si aplica)
- userAgent: String, agente de usuario (si aplica)

**Eventos a Registrar:**

| Tipo de Evento | Descripción | Actor | Entidad |
|----------------|---------------|-------|----------|
| TICKET_CREADO | Creación de nuevo ticket | Cliente/Sistema | Ticket |
| TICKET_ASIGNADO | Asignación a asesor | Sistema | Ticket |
| TICKET_COMPLETADO | Finalización de atención | Asesor/Sistema | Ticket |
| TICKET_CANCELADO | Cancelación de ticket | Cliente/Sistema | Ticket |
| MENSAJE_ENVIADO | Envío exitoso de mensaje | Sistema | Message |
| MENSAJE_FALLIDO | Fallo en envío de mensaje | Sistema | Message |
| ASESOR_CAMBIO_ESTADO | Cambio de estado de asesor | Asesor/Admin | Advisor |
| ACCESO_DASHBOARD | Acceso al panel administrativo | Supervisor | Sistema |
| COLA_ALERTA | Generación de alerta crítica | Sistema | Queue |

**Reglas de Negocio Aplicables:**
- RN-011: Auditoría obligatoria para todos los eventos críticos

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Registro de creación de ticket**
```gherkin
Given un cliente crea un ticket P05 a las 14:30:15
When el sistema procesa la creación exitosamente
Then el sistema registra en auditoría:
  {
    "timestamp": "2025-01-15T14:30:15.123Z",
    "tipoEvento": "TICKET_CREADO",
    "actor": "Cliente-12345678-9",
    "entityType": "Ticket",
    "entityId": "P05",
    "cambiosEstado": {
      "anterior": null,
      "nuevo": "EN_ESPERA"
    },
    "detallesAdicionales": {
      "queueType": "PERSONAL_BANKER",
      "positionInQueue": 3,
      "branchOffice": "Sucursal Centro"
    }
  }
And el registro se almacena inmediatamente en base de datos
```

**Escenario 2: Registro de asignación automática**
```gherkin
Given el ticket E07 se asigna automáticamente al asesor "Ana García"
When el sistema completa la asignación
Then el sistema registra:
  {
    "timestamp": "2025-01-15T14:35:42.567Z",
    "tipoEvento": "TICKET_ASIGNADO",
    "actor": "Sistema-AutoAssign",
    "entityType": "Ticket",
    "entityId": "E07",
    "cambiosEstado": {
      "anterior": "EN_ESPERA",
      "nuevo": "ATENDIENDO"
    },
    "detallesAdicionales": {
      "assignedAdvisor": "Ana García",
      "moduleNumber": 3,
      "assignmentReason": "BALANCEO_CARGA"
    }
  }
```

**Escenario 3: Registro de fallo en envío de mensaje**
```gherkin
Given un mensaje de Telegram falla después de 3 reintentos
When el sistema marca el mensaje como FALLIDO
Then el sistema registra:
  {
    "timestamp": "2025-01-15T14:40:18.890Z",
    "tipoEvento": "MENSAJE_FALLIDO",
    "actor": "Sistema-TelegramBot",
    "entityType": "Message",
    "entityId": "msg_12345",
    "cambiosEstado": {
      "anterior": "PENDIENTE",
      "nuevo": "FALLIDO"
    },
    "detallesAdicionales": {
      "plantilla": "totem_es_tu_turno",
      "intentos": 3,
      "ultimoError": "Network timeout",
      "ticketRelacionado": "C08"
    }
  }
```

**Escenario 4: Registro de acceso al dashboard**
```gherkin
Given el supervisor "Maria Rodriguez" accede al dashboard
When el sistema autentica y autoriza el acceso
Then el sistema registra:
  {
    "timestamp": "2025-01-15T14:45:00.123Z",
    "tipoEvento": "ACCESO_DASHBOARD",
    "actor": "Supervisor-maria.rodriguez",
    "entityType": "Sistema",
    "entityId": "dashboard",
    "detallesAdicionales": {
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0 Chrome/120.0",
      "sessionId": "sess_abc123"
    },
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0 Chrome/120.0"
  }
```

**Escenario 5: Registro de cambio de estado de asesor**
```gherkin
Given el asesor "Luis Torres" cambia su estado de AVAILABLE a OFFLINE
When el cambio se procesa en el sistema
Then el sistema registra:
  {
    "timestamp": "2025-01-15T12:00:00.000Z",
    "tipoEvento": "ASESOR_CAMBIO_ESTADO",
    "actor": "Asesor-luis.torres",
    "entityType": "Advisor",
    "entityId": "advisor_456",
    "cambiosEstado": {
      "anterior": "AVAILABLE",
      "nuevo": "OFFLINE"
    },
    "detallesAdicionales": {
      "razon": "ALMUERZO",
      "tiempoEstimado": 60,
      "moduleNumber": 2
    }
  }
```

**Postcondiciones:**
- Evento registrado inmediatamente en base de datos
- Información completa para trazabilidad
- Datos disponibles para auditorías y análisis
- Cumplimiento de normativas de trazabilidad

**Endpoints HTTP:**
- GET /api/health - Verificación de estado del sistema

---

## 5. Matriz de Trazabilidad

### 5.1 Matriz RF → Beneficio → Endpoints

| RF | Requerimiento | Beneficio de Negocio | Endpoints HTTP |
|----|---------------|---------------------|----------------|
| RF-001 | Crear Ticket Digital | Digitalización del proceso, eliminación de tickets físicos | POST /api/tickets |
| RF-002 | Notificaciones Telegram | Movilidad del cliente, reducción de abandonos | Ninguno (automatizado) |
| RF-003 | Calcular Posición y Tiempo | Transparencia, gestión de expectativas | GET /api/tickets/{numero}/position |
| RF-004 | Asignar Ticket Automáticamente | Eficiencia operacional, balanceo de carga | Ninguno (automatizado) |
| RF-005 | Gestionar Múltiples Colas | Segmentación de servicios, priorización | GET /api/admin/queues/{type}<br>GET /api/admin/queues/{type}/stats |
| RF-006 | Consultar Estado Ticket | Autoservicio, reducción de consultas | GET /api/tickets/{uuid}<br>GET /api/tickets/{numero}/position |
| RF-007 | Panel de Monitoreo | Supervisión operacional, toma de decisiones | GET /api/admin/dashboard<br>GET /api/admin/summary<br>GET /api/admin/advisors<br>GET /api/admin/advisors/stats<br>PUT /api/admin/advisors/{id}/status |
| RF-008 | Auditoría de Eventos | Cumplimiento normativo, trazabilidad | GET /api/health |

### 5.2 Matriz de Dependencias Entre RFs

| RF Origen | RF Dependiente | Tipo de Dependencia | Descripción |
|-----------|----------------|---------------------|-------------|
| RF-001 | RF-002 | Secuencial | Creación de ticket desencadena notificaciones |
| RF-001 | RF-003 | Simultánea | Creación requiere cálculo de posición |
| RF-001 | RF-008 | Simultánea | Creación genera evento de auditoría |
| RF-003 | RF-004 | Bidireccional | Asignación afecta posiciones, posiciones determinan asignación |
| RF-004 | RF-002 | Secuencial | Asignación desencadena Mensaje 3 |
| RF-004 | RF-008 | Simultánea | Asignación genera evento de auditoría |
| RF-005 | RF-001,RF-003,RF-004 | Fundamental | Gestión de colas es base para otros RFs |
| RF-007 | RF-001,RF-003,RF-004,RF-005 | Dependiente | Dashboard consume datos de otros RFs |
| RF-008 | Todos los RFs | Transversal | Auditoría registra eventos de todos los RFs |

---

## 6. Modelo de Datos Consolidado

### 6.1 Entidades Principales

**Entidad: Ticket**
- codigoReferencia: UUID (PK)
- numero: String
- nationalId: String
- telefono: String
- branchOffice: String
- queueType: Enum
- status: Enum
- positionInQueue: Integer
- estimatedWaitMinutes: Integer
- createdAt: Timestamp
- assignedAdvisor: FK a Advisor
- assignedModuleNumber: Integer

**Entidad: Advisor**
- id: BIGSERIAL (PK)
- name: String
- email: String
- status: Enum
- moduleNumber: Integer
- assignedTicketsCount: Integer
- queueTypes: Array
- lastAssignedAt: Timestamp

**Entidad: Message**
- id: BIGSERIAL (PK)
- ticket_id: FK a Ticket
- plantilla: String
- estadoEnvio: Enum
- fechaProgramada: Timestamp
- fechaEnvio: Timestamp
- telegramMessageId: String
- intentos: Integer

**Entidad: AuditLog**
- id: BIGSERIAL (PK)
- timestamp: Timestamp
- tipoEvento: String
- actor: String
- entityType: String
- entityId: String
- cambiosEstado: JSON
- detallesAdicionales: JSON
- ipAddress: String
- userAgent: String

---

## 7. Casos de Uso Principales

### CU-001: Flujo Completo de Atención
1. Cliente crea ticket (RF-001)
2. Sistema calcula posición (RF-003)
3. Sistema envía Mensaje 1 (RF-002)
4. Sistema monitorea progreso (RF-003)
5. Sistema envía Mensaje 2 cuando posición ≤ 3 (RF-002)
6. Sistema asigna a asesor disponible (RF-004)
7. Sistema envía Mensaje 3 (RF-002)
8. Cliente es atendido
9. Sistema registra auditoría en cada paso (RF-008)

### CU-002: Supervisión Operacional
1. Supervisor accede al dashboard (RF-007)
2. Sistema muestra estado de colas (RF-005)
3. Sistema muestra estado de asesores (RF-004)
4. Sistema genera alertas críticas (RF-007)
5. Supervisor toma acciones correctivas
6. Sistema registra accesos y cambios (RF-008)

### CU-003: Consulta de Cliente
1. Cliente consulta estado de ticket (RF-006)
2. Sistema calcula posición actualizada (RF-003)
3. Sistema retorna información en tiempo real
4. Sistema registra consulta (RF-008)

---

## 8. Matriz de Endpoints HTTP

| Método | Endpoint | RF | Descripción | Actor |
|--------|----------|----|--------------|---------|
| POST | /api/tickets | RF-001 | Crear nuevo ticket | Cliente |
| GET | /api/tickets/{uuid} | RF-006 | Consultar ticket por UUID | Cliente |
| GET | /api/tickets/{numero}/position | RF-003, RF-006 | Consultar posición por número | Cliente |
| GET | /api/admin/dashboard | RF-007 | Dashboard principal | Supervisor |
| GET | /api/admin/queues/{type} | RF-005 | Estado de cola específica | Supervisor |
| GET | /api/admin/queues/{type}/stats | RF-005 | Estadísticas de cola | Supervisor |
| GET | /api/admin/advisors | RF-007 | Estado de asesores | Supervisor |
| GET | /api/admin/advisors/stats | RF-007 | Estadísticas de asesores | Supervisor |
| PUT | /api/admin/advisors/{id}/status | RF-007 | Cambiar estado de asesor | Supervisor |
| GET | /api/admin/summary | RF-007 | Resumen operacional | Supervisor |
| GET | /api/health | RF-008 | Estado del sistema | Sistema |

---

## 9. Validaciones y Reglas de Formato

### 9.1 Validaciones de Entrada

**RUT/ID Nacional:**
- Formato: 12345678-9 (Chile) o equivalente según país
- Obligatorio para creación de ticket
- Validación de dígito verificador

**Teléfono:**
- Formato: +56XXXXXXXXX (internacional)
- Opcional (cliente puede optar por no recibir notificaciones)
- Validación de formato internacional

**Tipo de Cola:**
- Valores válidos: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA
- Obligatorio para creación de ticket

### 9.2 Reglas de Negocio Críticas

**Unicidad de Ticket Activo (RN-001):**
- Un cliente solo puede tener 1 ticket en estados: EN_ESPERA, PROXIMO, ATENDIENDO
- Validación antes de crear nuevo ticket

**Formato de Número (RN-005, RN-006):**
- Estructura: [Prefijo][01-99]
- Reinicio diario de numeración
- Prefijos únicos por cola

---

## 10. Checklist de Validación Final

### 10.1 Completitud
- ✅ 8 Requerimientos Funcionales documentados
- ✅ 47 Escenarios Gherkin totales (RF-001:7, RF-002:7, RF-003:6, RF-004:7, RF-005:5, RF-006:6, RF-007:6, RF-008:5)
- ✅ 13 Reglas de Negocio numeradas y aplicadas
- ✅ 11 Endpoints HTTP mapeados
- ✅ 4 Entidades definidas (Ticket, Advisor, Message, AuditLog)
- ✅ 4 Enumeraciones especificadas

### 10.2 Claridad
- ✅ Formato Gherkin correcto (Given/When/Then/And)
- ✅ Ejemplos JSON válidos en respuestas HTTP
- ✅ Sin ambigüedades en descripciones
- ✅ Precondiciones y postcondiciones claras

### 10.3 Trazabilidad
- ✅ Matriz RF → Beneficio → Endpoints completa
- ✅ Dependencias entre RFs identificadas
- ✅ Casos de uso principales documentados
- ✅ Reglas de negocio aplicadas a RFs correspondientes

### 10.4 Formato Profesional
- ✅ Numeración consistente (RF-XXX, RN-XXX)
- ✅ Tablas bien formateadas
- ✅ Jerarquía clara con ## y ###
- ✅ Sin menciones de tecnologías de implementación

---

## 11. Glosario

| Término | Definición |
|---------|------------|
| Backoff Exponencial | Técnica de reintentos con intervalos crecientes |
| FIFO | First In, First Out - Primero en entrar, primero en salir |
| Gherkin | Lenguaje de especificación de criterios de aceptación |
| UUID | Identificador único universal |
| Webhook | Mecanismo de notificación HTTP automática |
| Dashboard | Panel de control con métricas en tiempo real |
| API REST | Interfaz de programación de aplicaciones RESTful |
| JSON | Formato de intercambio de datos JavaScript Object Notation |
| Timestamp | Marca de tiempo con fecha y hora exacta |
| Enum | Enumeración de valores predefinidos |

---

**Documento completado exitosamente**  
**Total de páginas estimadas:** 65-70  
**Total de palabras:** ~14,500  
**Fecha de finalización:** Diciembre 2025

**Preparado por:** Analista de Negocio Senior  
**Revisado por:** Stakeholders del Proyecto  
**Aprobado para:** Diseño de Arquitectura y Desarrollo