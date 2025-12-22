# 📡 API Reference - Sistema Ticketero

## 🌐 Base URL

```
http://localhost:8080/api
```

## 🔐 Autenticación

Actualmente el sistema no requiere autenticación. En producción se recomienda implementar:
- JWT tokens
- API Keys
- OAuth 2.0

## 📋 Endpoints Overview

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/tickets` | Crear nuevo ticket | No |
| GET | `/tickets/{uuid}` | Obtener ticket por UUID | No |
| GET | `/tickets/{numero}/position` | Consultar posición en cola | No |
| GET | `/admin/dashboard` | Dashboard completo | No |
| GET | `/admin/advisors` | Lista de asesores | No |
| PUT | `/admin/advisors/{id}/status` | Cambiar estado asesor | No |
| POST | `/admin/tickets/{id}/complete` | Completar ticket | No |

---

## 🎫 Tickets API

### 1. Crear Ticket

**Endpoint:** `POST /api/tickets`

**Descripción:** Crea un nuevo ticket digital y envía notificación vía Telegram.

**Request Body:**
```json
{
  "tipoServicio": "CAJA",
  "telegramChatId": "1234567890"
}
```

**Campos:**
- `tipoServicio` (string, required): Tipo de servicio solicitado
  - Valores: `CAJA`, `PERSONAL_BANKER`, `EMPRESAS`, `GERENCIA`
- `telegramChatId` (string, required): ID del chat de Telegram (10 dígitos)

**Response 201 - Created:**
```json
{
  "id": 1,
  "numero": "C071234",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "tipoServicio": "CAJA",
  "estado": "ESPERANDO",
  "posicionEnCola": 3,
  "tiempoEstimadoMinutos": 15,
  "createdAt": "2024-12-07T10:30:00",
  "advisorAsignado": null,
  "telegramChatId": "1234567890"
}
```

**Response 400 - Bad Request:**
```json
{
  "message": "Validation failed",
  "status": 400,
  "timestamp": "2024-12-07T10:30:00",
  "errors": [
    "tipoServicio: must not be null",
    "telegramChatId: must match pattern ^[0-9]{10}$"
  ]
}
```

**Ejemplo cURL:**
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "tipoServicio": "CAJA",
    "telegramChatId": "1234567890"
  }'
```

---

### 2. Obtener Ticket por UUID

**Endpoint:** `GET /api/tickets/{uuid}`

**Descripción:** Obtiene información completa de un ticket usando su UUID.

**Path Parameters:**
- `uuid` (string, required): UUID único del ticket

**Response 200 - OK:**
```json
{
  "id": 1,
  "numero": "C071234",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "tipoServicio": "CAJA",
  "estado": "EN_PROGRESO",
  "posicionEnCola": 0,
  "tiempoEstimadoMinutos": 0,
  "createdAt": "2024-12-07T10:30:00",
  "advisorAsignado": {
    "id": 1,
    "nombre": "María González",
    "modulo": 3
  },
  "telegramChatId": "1234567890"
}
```

**Response 404 - Not Found:**
```json
{
  "message": "Ticket not found with UUID: 550e8400-e29b-41d4-a716-446655440000",
  "status": 404,
  "timestamp": "2024-12-07T10:30:00",
  "errors": []
}
```

**Ejemplo cURL:**
```bash
curl -X GET http://localhost:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000
```

---

### 3. Consultar Posición en Cola

**Endpoint:** `GET /api/tickets/{numero}/position`

**Descripción:** Consulta la posición actual de un ticket en la cola de espera.

**Path Parameters:**
- `numero` (string, required): Número del ticket (ej: "C071234")

**Response 200 - OK:**
```json
{
  "numero": "C071234",
  "posicionEnCola": 2,
  "tiempoEstimadoMinutos": 10,
  "estado": "ESPERANDO",
  "ticketsDelante": 2,
  "tiempoPromedioAtencion": 5
}
```

**Response 404 - Not Found:**
```json
{
  "message": "Ticket not found with number: C071234",
  "status": 404,
  "timestamp": "2024-12-07T10:30:00",
  "errors": []
}
```

**Ejemplo cURL:**
```bash
curl -X GET http://localhost:8080/api/tickets/C071234/position
```

---

## 👨‍💼 Admin API

### 1. Dashboard Completo

**Endpoint:** `GET /api/admin/dashboard`

**Descripción:** Obtiene estadísticas completas del sistema para el dashboard administrativo.

**Response 200 - OK:**
```json
{
  "estadisticas": {
    "ticketsEsperando": 5,
    "ticketsEnProgreso": 3,
    "ticketsCompletadosHoy": 47,
    "totalTicketsHoy": 55,
    "tiempoPromedioAtencion": 8.5,
    "asesoresDisponibles": 4,
    "asesoresOcupados": 3,
    "asesoresDescanso": 1
  },
  "colasPorTipo": {
    "CAJA": {
      "esperando": 2,
      "enProgreso": 1,
      "tiempoPromedio": 5.2
    },
    "PERSONAL_BANKER": {
      "esperando": 1,
      "enProgreso": 1,
      "tiempoPromedio": 12.8
    },
    "EMPRESAS": {
      "esperando": 1,
      "enProgreso": 1,
      "tiempoPromedio": 18.5
    },
    "GERENCIA": {
      "esperando": 1,
      "enProgreso": 0,
      "tiempoPromedio": 25.0
    }
  },
  "ticketsRecientes": [
    {
      "id": 55,
      "numero": "C071255",
      "tipoServicio": "CAJA",
      "estado": "ESPERANDO",
      "createdAt": "2024-12-07T11:45:00",
      "posicionEnCola": 1
    }
  ],
  "asesores": [
    {
      "id": 1,
      "nombre": "María González",
      "tipoServicio": "CAJA",
      "estado": "OCUPADO",
      "modulo": 3,
      "ticketActual": "C071254"
    }
  ]
}
```

**Ejemplo cURL:**
```bash
curl -X GET http://localhost:8080/api/admin/dashboard
```

---

### 2. Lista de Asesores

**Endpoint:** `GET /api/admin/advisors`

**Descripción:** Obtiene la lista completa de asesores con su estado actual.

**Response 200 - OK:**
```json
[
  {
    "id": 1,
    "nombre": "María González",
    "tipoServicio": "CAJA",
    "estado": "DISPONIBLE",
    "modulo": 3,
    "ticketsAtendidosHoy": 12,
    "tiempoPromedioAtencion": 5.2,
    "ultimaActividad": "2024-12-07T11:30:00"
  },
  {
    "id": 2,
    "nombre": "Carlos Rodríguez",
    "tipoServicio": "PERSONAL_BANKER",
    "estado": "OCUPADO",
    "modulo": 5,
    "ticketsAtendidosHoy": 8,
    "tiempoPromedioAtencion": 12.8,
    "ultimaActividad": "2024-12-07T11:45:00",
    "ticketActual": "PB071203"
  }
]
```

**Ejemplo cURL:**
```bash
curl -X GET http://localhost:8080/api/admin/advisors
```

---

### 3. Cambiar Estado de Asesor

**Endpoint:** `PUT /api/admin/advisors/{id}/status`

**Descripción:** Cambia el estado de un asesor (disponible, descanso, no disponible).

**Path Parameters:**
- `id` (integer, required): ID del asesor

**Request Body:**
```json
{
  "estado": "DESCANSO"
}
```

**Campos:**
- `estado` (string, required): Nuevo estado del asesor
  - Valores: `DISPONIBLE`, `OCUPADO`, `DESCANSO`, `NO_DISPONIBLE`

**Response 200 - OK:**
```json
{
  "id": 1,
  "nombre": "María González",
  "tipoServicio": "CAJA",
  "estado": "DESCANSO",
  "modulo": 3,
  "ultimaActividad": "2024-12-07T11:50:00"
}
```

**Response 404 - Not Found:**
```json
{
  "message": "Advisor not found with ID: 999",
  "status": 404,
  "timestamp": "2024-12-07T11:50:00",
  "errors": []
}
```

**Ejemplo cURL:**
```bash
curl -X PUT http://localhost:8080/api/admin/advisors/1/status \
  -H "Content-Type: application/json" \
  -d '{"estado": "DESCANSO"}'
```

---

### 4. Completar Ticket

**Endpoint:** `POST /api/admin/tickets/{id}/complete`

**Descripción:** Marca un ticket como completado y libera al asesor asignado.

**Path Parameters:**
- `id` (integer, required): ID del ticket

**Response 200 - OK:**
```json
{
  "id": 1,
  "numero": "C071234",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "tipoServicio": "CAJA",
  "estado": "COMPLETADO",
  "posicionEnCola": 0,
  "tiempoEstimadoMinutos": 0,
  "createdAt": "2024-12-07T10:30:00",
  "completedAt": "2024-12-07T10:38:00",
  "tiempoAtencionMinutos": 8,
  "advisorAsignado": {
    "id": 1,
    "nombre": "María González",
    "modulo": 3
  },
  "telegramChatId": "1234567890"
}
```

**Response 404 - Not Found:**
```json
{
  "message": "Ticket not found with ID: 999",
  "status": 404,
  "timestamp": "2024-12-07T11:50:00",
  "errors": []
}
```

**Response 400 - Bad Request:**
```json
{
  "message": "Ticket is not in progress, current state: ESPERANDO",
  "status": 400,
  "timestamp": "2024-12-07T11:50:00",
  "errors": []
}
```

**Ejemplo cURL:**
```bash
curl -X POST http://localhost:8080/api/admin/tickets/1/complete
```

---

## 🔍 Códigos de Estado HTTP

| Código | Descripción | Cuándo se usa |
|--------|-------------|---------------|
| 200 | OK | Operación exitosa |
| 201 | Created | Recurso creado exitosamente |
| 400 | Bad Request | Datos de entrada inválidos |
| 404 | Not Found | Recurso no encontrado |
| 500 | Internal Server Error | Error interno del servidor |

## ⚠️ Manejo de Errores

### Estructura de Error Estándar
```json
{
  "message": "Descripción del error",
  "status": 400,
  "timestamp": "2024-12-07T11:50:00",
  "errors": [
    "Campo específico: descripción del error"
  ]
}
```

### Errores Comunes

**Validación de Datos:**
```json
{
  "message": "Validation failed",
  "status": 400,
  "timestamp": "2024-12-07T11:50:00",
  "errors": [
    "tipoServicio: must not be null",
    "telegramChatId: must match pattern ^[0-9]{10}$"
  ]
}
```

**Recurso No Encontrado:**
```json
{
  "message": "Ticket not found with UUID: invalid-uuid",
  "status": 404,
  "timestamp": "2024-12-07T11:50:00",
  "errors": []
}
```

**Error Interno:**
```json
{
  "message": "Internal server error",
  "status": 500,
  "timestamp": "2024-12-07T11:50:00",
  "errors": []
}
```

## 📊 Rate Limiting

Actualmente no implementado. Recomendaciones para producción:

- **Tickets API**: 10 requests/minuto por IP
- **Admin API**: 100 requests/minuto por IP
- **Dashboard API**: 60 requests/minuto por IP

## 🧪 Testing con Postman

### Collection Variables
```json
{
  "baseUrl": "http://localhost:8080/api",
  "ticketUuid": "{{$guid}}",
  "telegramChatId": "1234567890"
}
```

### Pre-request Scripts
```javascript
// Generar UUID para tests
pm.globals.set("ticketUuid", pm.variables.replaceIn('{{$guid}}'));

// Timestamp actual
pm.globals.set("timestamp", new Date().toISOString());
```

### Test Scripts
```javascript
// Validar response exitoso
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

// Validar estructura de response
pm.test("Response has required fields", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('numero');
    pm.expect(jsonData).to.have.property('uuid');
    pm.expect(jsonData).to.have.property('estado');
});

// Guardar datos para siguientes requests
pm.test("Save ticket data", function () {
    const jsonData = pm.response.json();
    pm.globals.set("ticketId", jsonData.id);
    pm.globals.set("ticketNumber", jsonData.numero);
});
```

## 📱 Integración Frontend

### JavaScript/Fetch Example
```javascript
// Crear ticket
async function createTicket(tipoServicio, telegramChatId) {
  try {
    const response = await fetch('/api/tickets', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        tipoServicio,
        telegramChatId
      })
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error creating ticket:', error);
    throw error;
  }
}

// Obtener dashboard
async function getDashboard() {
  try {
    const response = await fetch('/api/admin/dashboard');
    return await response.json();
  } catch (error) {
    console.error('Error fetching dashboard:', error);
    throw error;
  }
}
```

### React Hook Example
```javascript
import { useState, useEffect } from 'react';

function useDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await fetch('/api/admin/dashboard');
        const data = await response.json();
        setDashboard(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
    const interval = setInterval(fetchDashboard, 30000); // Actualizar cada 30s

    return () => clearInterval(interval);
  }, []);

  return { dashboard, loading, error };
}
```

## 🔄 Versionado de API

**Versión Actual:** v1 (implícita)

**Estrategia de Versionado:**
- URL Path: `/api/v2/tickets`
- Header: `Accept: application/vnd.ticketero.v2+json`
- Query Parameter: `/api/tickets?version=2`

## 📋 Changelog API

### v1.0 (Actual)
- ✅ CRUD básico de tickets
- ✅ Dashboard administrativo
- ✅ Gestión de asesores
- ✅ Integración Telegram

### v1.1 (Planeado)
- 🔄 Autenticación JWT
- 🔄 Rate limiting
- 🔄 Paginación en listas
- 🔄 Filtros avanzados

---

**Versión API:** 1.0  
**Última actualización:** Diciembre 2024  
**Documentación:** Sistema Ticketero Team