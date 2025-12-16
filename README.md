# Sistema Ticketero Digital

Sistema de gestión de tickets con notificaciones en tiempo real vía Telegram para instituciones financieras.

## 🚀 Tecnologías

- **Java 21** (LTS)
- **Spring Boot 3.2.11**
- **PostgreSQL 16**
- **Flyway** (migraciones)
- **Telegram Bot API**
- **Docker & Docker Compose**
- **Maven 3.9+**

## 📋 Prerrequisitos

- Java 21 JDK
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (opcional, se puede usar Docker)

## 🛠️ Configuración de Desarrollo

### 1. Clonar el repositorio
```bash
git clone <repository-url>
cd ticketero
```

### 2. Configurar variables de entorno
Crear archivo `.env` en la raíz del proyecto:
```env
TELEGRAM_BOT_TOKEN=tu_bot_token_aqui
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123
SPRING_PROFILES_ACTIVE=dev
```

### 3. Levantar base de datos con Docker
```bash
docker-compose up postgres -d
```

### 4. Ejecutar migraciones
```bash
mvn flyway:migrate
```

### 5. Ejecutar la aplicación
```bash
mvn spring-boot:run
```

La aplicación estará disponible en: http://localhost:8080

### 6. Acceder al Dashboard
Abrir en navegador: http://localhost:8080/dashboard.html

## 🐳 Docker Compose (Desarrollo Completo)

Para levantar todo el stack (PostgreSQL + API):

```bash
# Configurar TELEGRAM_BOT_TOKEN en .env
echo "TELEGRAM_BOT_TOKEN=tu_token" > .env

# Levantar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f api
```

## 🧪 Testing

### Ejecutar todos los tests
```bash
mvn test
```

### Ejecutar tests con cobertura
```bash
mvn test jacoco:report
```

### Tests de integración
```bash
mvn test -Dtest="*IntegrationTest"
```

## 📊 Endpoints Principales

### API de Tickets
- `POST /api/tickets` - Crear ticket
- `GET /api/tickets/{uuid}` - Obtener ticket por UUID
- `GET /api/tickets/{numero}/position` - Consultar posición

### API Administrativa
- `GET /api/admin/dashboard` - Dashboard completo (JSON)
- `GET /dashboard.html` - Dashboard visual (HTML)
- `GET /api/admin/advisors` - Lista de asesores
- `PUT /api/admin/advisors/{id}/status` - Cambiar estado asesor
- `POST /api/admin/tickets/{id}/complete` - Completar ticket

### Actuator (Monitoreo)
- `GET /actuator/health` - Estado de la aplicación
- `GET /actuator/metrics` - Métricas de performance

## 🗄️ Base de Datos

### Estructura de tablas
- `ticket` - Tickets del sistema
- `advisor` - Asesores/ejecutivos
- `mensaje` - Mensajes programados para Telegram

### Migraciones
Las migraciones se ejecutan automáticamente al iniciar la aplicación:
- `V1__create_tables.sql` - Tablas principales
- `V2__create_indexes.sql` - Índices de performance
- `V3__insert_sample_data.sql` - Datos de ejemplo

## 📱 Integración Telegram

### Configurar Bot
1. Crear bot con @BotFather en Telegram
2. Obtener token del bot
3. Configurar token en variable `TELEGRAM_BOT_TOKEN`

### Plantillas de mensajes
- **Confirmación:** "✅ Ticket {numero}, posición #{posicion}, {tiempo}min"
- **Pre-aviso:** "⏰ Pronto será tu turno {numero}"
- **Turno activo:** "🔔 ES TU TURNO {numero}! Módulo {modulo}"

## 🔧 Configuración

### Profiles disponibles
- `dev` - Desarrollo (logs detallados)
- `prod` - Producción (logs optimizados)
- `test` - Testing (H2 en memoria)

### Variables de entorno importantes
| Variable | Descripción | Requerido |
|----------|-------------|-----------|
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram | Sí |
| `DATABASE_URL` | URL de PostgreSQL | Sí |
| `DATABASE_USERNAME` | Usuario de BD | Sí |
| `DATABASE_PASSWORD` | Password de BD | Sí |

## 📈 Monitoreo

### Health Checks
```bash
curl http://localhost:8080/actuator/health
```

### Métricas
```bash
curl http://localhost:8080/actuator/metrics
```

### Logs
Los logs se escriben en formato estructurado:
```bash
docker-compose logs -f api
```

## 🚀 Deployment

### Build para producción
```bash
mvn clean package -DskipTests
```

### Docker build
```bash
docker build -t ticketero:latest .
```

### Deploy con Docker Compose
```bash
# Configurar variables de producción
cp .env.example .env.prod

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

## 🐛 Troubleshooting

### Problemas comunes

**Error de conexión a BD:**
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Ver logs de BD
docker-compose logs postgres
```

**Error de Telegram:**
```bash
# Verificar token
echo $TELEGRAM_BOT_TOKEN

# Ver logs de la aplicación
docker-compose logs api | grep -i telegram
```

**Migraciones fallan:**
```bash
# Limpiar BD y reiniciar
docker-compose down -v
docker-compose up postgres -d
mvn flyway:clean flyway:migrate
```

## 📚 Documentación Adicional

- [Arquitectura del Sistema](docs/ARQUITECTURA.md)
- [Plan de Implementación](docs/PLAN-IMPLEMENTACION.md)
- [Requerimientos Funcionales](docs/REQUERIMIENTOS-FUNCIONALES.md)

## 🤝 Contribución

1. Fork del proyecto
2. Crear feature branch (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push al branch (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto es para fines educativos y de capacitación.

---

**Estado del Proyecto:** Sprint 3 Completado ✅  
**Funcionalidades Implementadas:**
- ✅ RF-001: Crear Ticket Digital
- ✅ RF-002: Enviar Notificaciones Automáticas vía Telegram
- ✅ RF-003: Calcular Posición y Tiempo Estimado  
- ✅ RF-004: Asignar Ticket a Ejecutivo Automáticamente
- ✅ RF-006: Consultar Estado del Ticket
- ✅ RF-007: Panel de Monitoreo para Supervisor
- ✅ RF-008: Registrar Auditoría de Eventos
- ✅ Arquitectura base con Spring Boot 3.2.11 + Java 21
- ✅ Entidades JPA con relaciones
- ✅ DTOs con Records y validación
- ✅ Repositorios con queries derivadas
- ✅ Migraciones Flyway
- ✅ Integración Telegram Bot API
- ✅ Sistema de asignación automática
- ✅ Scheduler para procesamiento de colas
- ✅ Panel administrativo básico
- ✅ Dashboard en tiempo real con HTML
- ✅ Sistema de auditoría completo
- ✅ Alertas automáticas
- ✅ Tests unitarios e integración

**Proyecto Completado:** Todas las funcionalidades core implementadas