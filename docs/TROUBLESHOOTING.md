# 🔧 Troubleshooting Guide - Sistema Ticketero

## 🚨 Problemas Críticos

### 1. Aplicación No Inicia

**Síntomas:**
- Error al ejecutar `mvn spring-boot:run`
- Container Docker no se levanta
- Puerto 8080 no responde

**Diagnóstico:**
```bash
# Verificar Java
java -version
# Debe mostrar Java 21

# Verificar puerto ocupado
netstat -tulpn | grep :8080
lsof -i :8080

# Verificar logs
tail -f logs/application.log
docker-compose logs api
```

**Soluciones:**

**A. Java Version Incorrecta**
```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk
sudo update-alternatives --config java

# macOS
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21

# Windows
# Descargar e instalar OpenJDK 21
# Configurar JAVA_HOME en variables de entorno
```

**B. Puerto Ocupado**
```bash
# Encontrar proceso que usa el puerto
sudo lsof -i :8080
sudo kill -9 <PID>

# O cambiar puerto
export SERVER_PORT=8081
# O en application.yml: server.port: 8081
```

**C. Dependencias Maven Corruptas**
```bash
# Limpiar cache Maven
mvn clean
rm -rf ~/.m2/repository
mvn dependency:resolve
```

### 2. Error de Conexión a Base de Datos

**Síntomas:**
- `Connection refused` en logs
- `Unable to obtain connection from database`
- Tests fallan con errores de BD

**Diagnóstico:**
```bash
# Verificar PostgreSQL corriendo
docker-compose ps postgres
sudo systemctl status postgresql

# Test de conectividad
telnet localhost 5432
pg_isready -h localhost -p 5432

# Verificar variables de entorno
echo $DATABASE_URL
echo $DATABASE_USERNAME
echo $DATABASE_PASSWORD
```

**Soluciones:**

**A. PostgreSQL No Está Corriendo**
```bash
# Docker
docker-compose up postgres -d

# Sistema local
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**B. Credenciales Incorrectas**
```bash
# Verificar usuario existe
sudo -u postgres psql -c "\du"

# Crear usuario si no existe
sudo -u postgres createuser --interactive ticketero_user
sudo -u postgres psql -c "ALTER USER ticketero_user PASSWORD 'new_password';"

# Verificar base de datos existe
sudo -u postgres psql -c "\l"

# Crear base de datos si no existe
sudo -u postgres createdb ticketero
```

**C. Configuración de Conexión**
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ticketero
    username: ticketero_user
    password: correct_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

### 3. Error de Telegram Bot

**Síntomas:**
- `Telegram API error: 401 Unauthorized`
- `Failed to send message to chat`
- Notificaciones no llegan

**Diagnóstico:**
```bash
# Verificar token
curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe"

# Verificar conectividad
curl -I https://api.telegram.org

# Ver logs específicos
docker-compose logs api | grep -i telegram
```

**Soluciones:**

**A. Token Inválido**
```bash
# Verificar token con BotFather
# 1. Ir a @BotFather en Telegram
# 2. Enviar /mybots
# 3. Seleccionar bot
# 4. API Token

# Actualizar variable de entorno
export TELEGRAM_BOT_TOKEN=new_correct_token
```

**B. Chat ID Inválido**
```bash
# El usuario debe enviar /start al bot primero
# Verificar formato: debe ser 10 dígitos numéricos
# No puede empezar con 0
```

**C. Rate Limiting**
```bash
# Telegram límites:
# - 30 mensajes por segundo
# - 1 mensaje por chat por segundo

# Verificar logs de rate limiting
grep "rate limit" logs/application.log
```

## ⚠️ Problemas Comunes

### 1. Migraciones Flyway Fallan

**Síntomas:**
- `FlywayException: Validate failed`
- `Migration checksum mismatch`
- Aplicación no inicia por migraciones

**Diagnóstico:**
```bash
# Ver estado de migraciones
mvn flyway:info

# Ver historial en BD
docker exec -it ticketero-postgres psql -U dev_user -d ticketero -c "SELECT * FROM flyway_schema_history;"
```

**Soluciones:**

**A. Checksum Mismatch**
```bash
# Reparar checksums (CUIDADO: solo en desarrollo)
mvn flyway:repair

# En producción: crear nueva migración
# V4__fix_previous_migration.sql
```

**B. Migración Fallida**
```bash
# Limpiar BD (SOLO DESARROLLO)
mvn flyway:clean
mvn flyway:migrate

# Producción: rollback manual y nueva migración
```

**C. Orden de Migraciones Incorrecto**
```bash
# Verificar nombres de archivos
ls -la src/main/resources/db/migration/
# Deben ser: V1__description.sql, V2__description.sql, etc.
```

### 2. Tests Fallan

**Síntomas:**
- `mvn test` falla
- Tests de integración no pasan
- Coverage bajo

**Diagnóstico:**
```bash
# Ejecutar test específico
mvn test -Dtest=TicketServiceTest

# Ver logs detallados
mvn test -X

# Verificar perfil de test
mvn test -Dspring.profiles.active=test
```

**Soluciones:**

**A. H2 Database Issues**
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**B. TestContainers Problems**
```bash
# Verificar Docker corriendo
docker ps

# Limpiar containers
docker system prune -f

# Verificar permisos Docker
sudo usermod -aG docker $USER
```

**C. Mock Configuration**
```java
// Verificar mocks están configurados correctamente
@MockBean
private TelegramService telegramService;

// No @Mock en tests de integración
```

### 3. Performance Issues

**Síntomas:**
- Respuestas lentas (>2 segundos)
- Alto uso de CPU/memoria
- Timeouts en requests

**Diagnóstico:**
```bash
# Monitorear recursos
top -p $(pgrep java)
htop

# Ver métricas de aplicación
curl http://localhost:8080/actuator/metrics

# Analizar queries lentas
docker exec -it ticketero-postgres psql -U dev_user -d ticketero -c "
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;"
```

**Soluciones:**

**A. Query Optimization**
```sql
-- Verificar índices
EXPLAIN ANALYZE SELECT * FROM ticket WHERE estado = 'ESPERANDO';

-- Crear índices faltantes
CREATE INDEX idx_ticket_estado_created ON ticket(estado, created_at);

-- Actualizar estadísticas
ANALYZE ticket;
```

**B. JVM Tuning**
```bash
# Aumentar memoria heap
export JAVA_OPTS="-Xmx2g -Xms1g"

# Usar G1GC para mejor performance
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"

# Habilitar JFR para profiling
export JAVA_OPTS="$JAVA_OPTS -XX:+FlightRecorder"
```

**C. Connection Pool Tuning**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 4. Docker Issues

**Síntomas:**
- `docker-compose up` falla
- Containers se reinician constantemente
- Volúmenes no persisten datos

**Diagnóstico:**
```bash
# Ver estado de containers
docker-compose ps

# Ver logs
docker-compose logs api
docker-compose logs postgres

# Verificar recursos
docker stats

# Verificar volúmenes
docker volume ls
docker volume inspect ticketero_postgres_data
```

**Soluciones:**

**A. Memory/CPU Limits**
```yaml
# docker-compose.yml
services:
  api:
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
```

**B. Health Checks Failing**
```yaml
# Ajustar health check
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 60s  # Aumentar intervalo
  timeout: 30s   # Aumentar timeout
  retries: 5     # Más reintentos
  start_period: 120s  # Más tiempo para iniciar
```

**C. Volume Permissions**
```bash
# Verificar permisos
ls -la /var/lib/docker/volumes/

# Cambiar ownership
sudo chown -R 999:999 /var/lib/docker/volumes/ticketero_postgres_data/
```

## 🔍 Herramientas de Diagnóstico

### 1. Health Checks

```bash
# Health general
curl http://localhost:8080/actuator/health

# Health detallado
curl http://localhost:8080/actuator/health | jq

# Componentes específicos
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/diskSpace
```

### 2. Métricas y Monitoreo

```bash
# Métricas JVM
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Métricas de aplicación
curl http://localhost:8080/actuator/metrics/http.server.requests

# Métricas de base de datos
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Thread dump
curl http://localhost:8080/actuator/threaddump

# Heap dump (CUIDADO: archivo grande)
curl http://localhost:8080/actuator/heapdump -o heapdump.hprof
```

### 3. Logging Avanzado

```yaml
# application-debug.yml
logging:
  level:
    com.ticketero: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.security: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 4. Database Diagnostics

```sql
-- Conexiones activas
SELECT pid, usename, application_name, client_addr, state, query_start, query 
FROM pg_stat_activity 
WHERE state = 'active';

-- Queries más lentas
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

-- Tamaño de tablas
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Índices no utilizados
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE idx_tup_read = 0;

-- Locks activos
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS current_statement_in_blocking_process
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

## 🚨 Procedimientos de Emergencia

### 1. Aplicación Caída

**Pasos Inmediatos:**
```bash
# 1. Verificar estado
curl -f http://localhost:8080/actuator/health || echo "App DOWN"

# 2. Reiniciar aplicación
docker-compose restart api
# O
sudo systemctl restart ticketero

# 3. Verificar logs
docker-compose logs --tail=100 api

# 4. Verificar recursos del sistema
free -h
df -h
top
```

**Si No Responde:**
```bash
# 1. Forzar reinicio
docker-compose down
docker-compose up -d

# 2. Verificar base de datos
docker-compose exec postgres pg_isready

# 3. Verificar conectividad externa
curl -I https://api.telegram.org

# 4. Rollback si es necesario
git log --oneline -10
git checkout <previous-commit>
docker-compose up -d --build
```

### 2. Base de Datos Corrupta

**Diagnóstico:**
```bash
# Verificar integridad
docker-compose exec postgres pg_dump ticketero > /dev/null

# Verificar logs de PostgreSQL
docker-compose logs postgres | grep -i error
```

**Recovery:**
```bash
# 1. Detener aplicación
docker-compose stop api

# 2. Backup actual (por si acaso)
docker-compose exec postgres pg_dump -U dev_user ticketero > emergency_backup.sql

# 3. Restaurar desde backup más reciente
docker-compose exec postgres psql -U dev_user -d ticketero < latest_backup.sql

# 4. Verificar integridad
docker-compose exec postgres psql -U dev_user -d ticketero -c "SELECT COUNT(*) FROM ticket;"

# 5. Reiniciar aplicación
docker-compose start api
```

### 3. Disco Lleno

**Diagnóstico:**
```bash
# Verificar espacio
df -h

# Encontrar archivos grandes
du -sh /var/lib/docker/volumes/*
du -sh logs/*
```

**Limpieza:**
```bash
# Limpiar logs antiguos
find logs/ -name "*.log" -mtime +7 -delete

# Limpiar Docker
docker system prune -f
docker volume prune -f

# Limpiar backups antiguos
find backup/ -name "*.sql" -mtime +30 -delete

# Rotar logs
logrotate -f /etc/logrotate.d/ticketero
```

## 📞 Escalation Matrix

### Nivel 1 - Desarrollador
**Responsabilidades:**
- Bugs de aplicación
- Problemas de configuración
- Issues de desarrollo local

**Contacto:** developer@company.com

### Nivel 2 - DevOps/SRE
**Responsabilidades:**
- Problemas de infraestructura
- Issues de deployment
- Performance problems
- Monitoreo y alertas

**Contacto:** devops@company.com

### Nivel 3 - Arquitecto/Lead
**Responsabilidades:**
- Decisiones arquitectónicas
- Problemas complejos de diseño
- Escalabilidad crítica

**Contacto:** architect@company.com

### Nivel 4 - Management
**Responsabilidades:**
- Outages críticos
- Decisiones de negocio
- Comunicación con stakeholders

**Contacto:** manager@company.com

## 📋 Runbooks

### 1. Restart Completo del Sistema

```bash
#!/bin/bash
# restart-system.sh

echo "🔄 Iniciando restart completo del sistema..."

# 1. Backup preventivo
echo "📦 Creando backup preventivo..."
./backup.sh emergency

# 2. Detener servicios
echo "⏹️ Deteniendo servicios..."
docker-compose down

# 3. Limpiar recursos
echo "🧹 Limpiando recursos..."
docker system prune -f

# 4. Verificar volúmenes
echo "💾 Verificando volúmenes..."
docker volume ls | grep ticketero

# 5. Reiniciar servicios
echo "🚀 Reiniciando servicios..."
docker-compose up -d

# 6. Verificar health
echo "🔍 Verificando health..."
sleep 30
curl -f http://localhost:8080/actuator/health

echo "✅ Restart completo finalizado"
```

### 2. Rollback de Deployment

```bash
#!/bin/bash
# rollback.sh

PREVIOUS_VERSION=$1

if [ -z "$PREVIOUS_VERSION" ]; then
    echo "Usage: ./rollback.sh <version>"
    exit 1
fi

echo "🔄 Iniciando rollback a versión $PREVIOUS_VERSION..."

# 1. Backup actual
./backup.sh pre-rollback

# 2. Checkout versión anterior
git checkout $PREVIOUS_VERSION

# 3. Build imagen
docker build -t ticketero:$PREVIOUS_VERSION .

# 4. Update docker-compose
sed -i "s/ticketero:latest/ticketero:$PREVIOUS_VERSION/g" docker-compose.yml

# 5. Deploy
docker-compose up -d --no-deps api

# 6. Verificar
sleep 30
curl -f http://localhost:8080/actuator/health

echo "✅ Rollback completado"
```

### 3. Limpieza de Emergencia

```bash
#!/bin/bash
# emergency-cleanup.sh

echo "🚨 Iniciando limpieza de emergencia..."

# Detener aplicación
docker-compose stop api

# Limpiar logs
find logs/ -name "*.log" -mtime +1 -delete
truncate -s 0 logs/application.log

# Limpiar Docker
docker system prune -af
docker volume prune -f

# Limpiar base de datos (datos antiguos)
docker-compose exec postgres psql -U dev_user -d ticketero -c "
DELETE FROM mensaje WHERE created_at < NOW() - INTERVAL '7 days';
DELETE FROM ticket WHERE estado = 'COMPLETADO' AND completed_at < NOW() - INTERVAL '30 days';
VACUUM ANALYZE;
"

# Reiniciar aplicación
docker-compose start api

echo "✅ Limpieza completada"
```

## 📚 FAQ Técnico

### Q: ¿Por qué los tickets no se procesan automáticamente?

**A:** Verificar:
1. Scheduler habilitado: `@EnableScheduling`
2. Asesores disponibles: `SELECT * FROM advisor WHERE estado = 'DISPONIBLE'`
3. Logs del scheduler: `grep "TicketProcessingScheduler" logs/application.log`

### Q: ¿Por qué las notificaciones de Telegram no llegan?

**A:** Verificar:
1. Token válido: `curl "https://api.telegram.org/bot$TOKEN/getMe"`
2. Chat ID correcto: debe ser 10 dígitos, no empezar con 0
3. Usuario envió `/start` al bot
4. Rate limiting: máximo 30 mensajes/segundo

### Q: ¿Cómo aumentar el performance?

**A:** Optimizaciones:
1. Aumentar pool de conexiones: `hikari.maximum-pool-size: 20`
2. Crear índices: `CREATE INDEX idx_ticket_estado_created ON ticket(estado, created_at)`
3. Aumentar memoria JVM: `-Xmx2g`
4. Usar G1GC: `-XX:+UseG1GC`

### Q: ¿Cómo hacer backup en producción?

**A:** Proceso:
```bash
# Backup automático
0 2 * * * /opt/ticketero/backup.sh daily

# Backup manual
pg_dump -h localhost -U ticketero_user ticketero > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Q: ¿Cómo monitorear el sistema?

**A:** Herramientas:
1. Health checks: `/actuator/health`
2. Métricas: `/actuator/metrics`
3. Logs: `tail -f logs/application.log`
4. Dashboard: `http://localhost:8080/dashboard.html`

---

**Versión:** 1.0  
**Última actualización:** Diciembre 2024  
**Support Team:** Sistema Ticketero