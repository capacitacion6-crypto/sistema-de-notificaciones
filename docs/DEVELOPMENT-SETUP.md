# 🛠️ Development Setup - Sistema Ticketero

## 📋 Prerrequisitos

### Software Requerido
- **Java 21 JDK** (OpenJDK recomendado)
- **Maven 3.9+**
- **Git 2.30+**
- **Docker 24.0+** y **Docker Compose 2.0+**
- **PostgreSQL 16** (opcional, se puede usar Docker)
- **IDE** (IntelliJ IDEA, VS Code, Eclipse)

### Verificar Instalaciones
```bash
# Java
java -version
# Debe mostrar: openjdk version "21.0.x"

# Maven
mvn -version
# Debe mostrar: Apache Maven 3.9.x

# Docker
docker --version
docker-compose --version

# Git
git --version
```

## 🚀 Configuración Inicial

### 1. Clonar Repositorio
```bash
git clone https://github.com/your-org/sistema-ticketero.git
cd sistema-ticketero
```

### 2. Configurar Variables de Entorno

**Crear archivo `.env` (desarrollo local):**
```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero_dev
DATABASE_USERNAME=dev_user
DATABASE_PASSWORD=dev_password

# Telegram Bot
TELEGRAM_BOT_TOKEN=your_development_bot_token_here

# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_TICKETERO=DEBUG

# JVM Options (desarrollo)
JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC
```

**Para Windows, crear `setenv.bat`:**
```batch
@echo off
set DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero_dev
set DATABASE_USERNAME=dev_user
set DATABASE_PASSWORD=dev_password
set TELEGRAM_BOT_TOKEN=your_development_bot_token_here
set SPRING_PROFILES_ACTIVE=dev
echo Environment variables set for development
```

### 3. Configurar Base de Datos

**Opción A: Docker (Recomendado)**
```bash
# Levantar solo PostgreSQL
docker-compose up postgres -d

# Verificar que esté corriendo
docker-compose ps postgres
```

**Opción B: PostgreSQL Local**
```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib

# macOS
brew install postgresql

# Windows
# Descargar desde https://www.postgresql.org/download/windows/

# Crear base de datos
sudo -u postgres createdb ticketero_dev
sudo -u postgres createuser dev_user
sudo -u postgres psql -c "ALTER USER dev_user PASSWORD 'dev_password';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE ticketero_dev TO dev_user;"
```

## 🔧 Configuración de IDE

### IntelliJ IDEA (Recomendado)

**1. Importar Proyecto:**
- File → Open → Seleccionar carpeta del proyecto
- Seleccionar "Import as Maven project"

**2. Configurar JDK:**
- File → Project Structure → Project
- Project SDK: Java 21
- Project language level: 21

**3. Plugins Recomendados:**
```
- Lombok Plugin (OBLIGATORIO)
- Spring Boot Plugin
- Database Navigator
- Docker Plugin
- GitToolBox
- SonarLint
- Rainbow Brackets
```

**4. Configurar Lombok:**
- File → Settings → Build → Compiler → Annotation Processors
- ✅ Enable annotation processing

**5. Configurar Code Style:**
```
File → Settings → Editor → Code Style → Java
- Importar: docs/intellij-code-style.xml
```

**6. Run Configuration:**
```
Name: TicketeroApplication
Main class: com.ticketero.TicketeroApplication
VM options: -Dspring.profiles.active=dev
Environment variables: (cargar desde .env)
```

### VS Code

**1. Extensiones Recomendadas:**
```json
{
  "recommendations": [
    "vscjava.vscode-java-pack",
    "vmware.vscode-spring-boot",
    "gabrielbb.vscode-lombok",
    "ms-vscode.vscode-docker",
    "eamodio.gitlens",
    "sonarsource.sonarlint-vscode"
  ]
}
```

**2. Configuración (settings.json):**
```json
{
  "java.home": "/path/to/java-21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java-21"
    }
  ],
  "spring-boot.ls.java.home": "/path/to/java-21",
  "java.format.settings.url": "docs/eclipse-formatter.xml"
}
```

**3. Launch Configuration (.vscode/launch.json):**
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "TicketeroApplication",
      "request": "launch",
      "mainClass": "com.ticketero.TicketeroApplication",
      "projectName": "ticketero",
      "env": {
        "SPRING_PROFILES_ACTIVE": "dev",
        "DATABASE_URL": "jdbc:postgresql://localhost:5432/ticketero_dev",
        "TELEGRAM_BOT_TOKEN": "your_token_here"
      }
    }
  ]
}
```

### Eclipse

**1. Importar Proyecto:**
- File → Import → Existing Maven Projects
- Seleccionar carpeta del proyecto

**2. Configurar Lombok:**
- Descargar lombok.jar
- Ejecutar: `java -jar lombok.jar`
- Instalar en Eclipse

**3. Configurar JDK:**
- Project → Properties → Java Build Path
- Libraries → Modulepath → Add Library → JRE System Library
- Seleccionar Java 21

## 🏃‍♂️ Ejecutar la Aplicación

### 1. Preparar Base de Datos
```bash
# Ejecutar migraciones
mvn flyway:migrate

# Verificar tablas creadas
docker exec -it ticketero-postgres psql -U dev_user -d ticketero_dev -c "\dt"
```

### 2. Compilar y Ejecutar

**Opción A: Maven**
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar aplicación
mvn spring-boot:run
```

**Opción B: IDE**
- Ejecutar `TicketeroApplication.main()`
- O usar Run Configuration configurada

**Opción C: JAR**
```bash
# Compilar JAR
mvn clean package -DskipTests

# Ejecutar JAR
java -jar target/ticketero-*.jar
```

### 3. Verificar Funcionamiento
```bash
# Health check
curl http://localhost:8080/actuator/health

# Dashboard
curl http://localhost:8080/api/admin/dashboard

# Crear ticket de prueba
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"tipoServicio": "CAJA", "telegramChatId": "1234567890"}'
```

## 🧪 Configuración de Testing

### 1. Base de Datos de Test

**application-test.yml:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  h2:
    console:
      enabled: true

telegram:
  bot:
    token: test_token_123
    api-url: http://localhost:8089/bot
```

### 2. TestContainers (Recomendado)

**Dependencia Maven:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

**Configuración Base:**
```java
@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketero_test")
            .withUsername("test_user")
            .withPassword("test_password");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### 3. Perfiles de Test

**Test con H2 (rápido):**
```bash
mvn test -Dspring.profiles.active=test
```

**Test con PostgreSQL (realista):**
```bash
mvn test -Dspring.profiles.active=test-postgres
```

**Test de integración completa:**
```bash
mvn test -Dtest="*IntegrationTest"
```

## 🔧 Herramientas de Desarrollo

### 1. Hot Reload

**Spring Boot DevTools (ya incluido):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Configuración:**
```yaml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

### 2. Database Tools

**Acceso a H2 Console (test):**
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: (vacío)
```

**pgAdmin (PostgreSQL):**
```bash
docker run -p 5050:80 \
  -e PGADMIN_DEFAULT_EMAIL=admin@ticketero.com \
  -e PGADMIN_DEFAULT_PASSWORD=admin \
  dpage/pgadmin4
```

**DBeaver (Recomendado):**
- Descargar desde https://dbeaver.io/
- Configurar conexión a PostgreSQL local

### 3. API Testing

**Postman Collection:**
```bash
# Importar colección
docs/postman/Ticketero-API.postman_collection.json

# Variables de entorno
docs/postman/Development.postman_environment.json
```

**HTTPie (CLI):**
```bash
# Instalar
pip install httpie

# Crear ticket
http POST localhost:8080/api/tickets tipoServicio=CAJA telegramChatId=1234567890

# Dashboard
http GET localhost:8080/api/admin/dashboard
```

### 4. Logging y Debugging

**Configurar logs detallados:**
```yaml
logging:
  level:
    com.ticketero: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Debug remoto:**
```bash
# Ejecutar con debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Conectar desde IDE al puerto 5005
```

## 📊 Monitoreo en Desarrollo

### 1. Actuator Endpoints
```bash
# Health
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Info de la aplicación
curl http://localhost:8080/actuator/info

# Beans de Spring
curl http://localhost:8080/actuator/beans
```

### 2. Prometheus Metrics
```bash
# Métricas en formato Prometheus
curl http://localhost:8080/actuator/prometheus
```

### 3. Dashboard de Desarrollo
```
# Dashboard HTML
http://localhost:8080/dashboard.html

# API del dashboard
http://localhost:8080/api/admin/dashboard
```

## 🔄 Workflow de Desarrollo

### 1. Flujo Git
```bash
# Crear feature branch
git checkout -b feature/nueva-funcionalidad

# Hacer cambios y commits
git add .
git commit -m "feat: agregar nueva funcionalidad"

# Push y crear PR
git push origin feature/nueva-funcionalidad
```

### 2. Pre-commit Hooks

**Instalar pre-commit:**
```bash
pip install pre-commit
pre-commit install
```

**.pre-commit-config.yaml:**
```yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.4.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json
  
  - repo: local
    hooks:
      - id: maven-test
        name: Maven Test
        entry: mvn test
        language: system
        pass_filenames: false
```

### 3. Code Quality

**Checkstyle:**
```bash
mvn checkstyle:check
```

**SpotBugs:**
```bash
mvn spotbugs:check
```

**SonarQube (local):**
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000
```

## 🐛 Troubleshooting

### Problemas Comunes

**1. Lombok no funciona:**
```bash
# Verificar plugin instalado en IDE
# Verificar annotation processing habilitado
# Reimportar proyecto Maven
```

**2. Base de datos no conecta:**
```bash
# Verificar PostgreSQL corriendo
docker-compose ps postgres

# Verificar variables de entorno
echo $DATABASE_URL

# Test de conexión
telnet localhost 5432
```

**3. Puerto 8080 ocupado:**
```bash
# Encontrar proceso
lsof -i :8080
netstat -tulpn | grep :8080

# Cambiar puerto
export SERVER_PORT=8081
```

**4. Tests fallan:**
```bash
# Limpiar y recompilar
mvn clean compile

# Ejecutar test específico
mvn test -Dtest=TicketServiceTest

# Ver logs detallados
mvn test -X
```

**5. Hot reload no funciona:**
```bash
# Verificar DevTools en classpath
# Reiniciar IDE
# Verificar configuración de build automático
```

### Comandos Útiles

```bash
# Limpiar todo
mvn clean
docker-compose down -v

# Reiniciar desarrollo
docker-compose up postgres -d
mvn flyway:migrate
mvn spring-boot:run

# Ver logs en tiempo real
tail -f logs/application.log

# Monitorear base de datos
docker exec -it ticketero-postgres psql -U dev_user -d ticketero_dev

# Backup de desarrollo
pg_dump -h localhost -U dev_user ticketero_dev > backup_dev.sql
```

## 📚 Recursos Adicionales

### Documentación
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Lombok Documentation](https://projectlombok.org/features/all)
- [Flyway Documentation](https://flywaydb.org/documentation/)

### Tutoriales
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [TestContainers](https://www.testcontainers.org/quickstart/junit_5_quickstart/)
- [Docker Compose](https://docs.docker.com/compose/gettingstarted/)

### Herramientas Online
- [Spring Initializr](https://start.spring.io/)
- [JSON Formatter](https://jsonformatter.curiousconcept.com/)
- [Regex Tester](https://regex101.com/)
- [Cron Expression Generator](https://crontab.guru/)

---

**Versión:** 1.0  
**Última actualización:** Diciembre 2024  
**Development Team:** Sistema Ticketero