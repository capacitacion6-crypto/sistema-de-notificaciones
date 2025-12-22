# 🚀 Deployment Guide - Sistema Ticketero

## 📋 Prerrequisitos de Producción

### Infraestructura Mínima
- **CPU**: 2 cores (4 recomendado)
- **RAM**: 4GB (8GB recomendado)
- **Storage**: 50GB SSD
- **OS**: Ubuntu 20.04+ / CentOS 8+ / RHEL 8+
- **Java**: OpenJDK 21 LTS
- **PostgreSQL**: 16+
- **Docker**: 24.0+ (opcional pero recomendado)

### Puertos Requeridos
- **8080**: Aplicación Spring Boot
- **5432**: PostgreSQL
- **80/443**: Nginx (proxy reverso)
- **9090**: Prometheus (monitoreo)
- **3000**: Grafana (dashboards)

---

## 🐳 Deployment con Docker (Recomendado)

### 1. Preparar Archivos de Configuración

**docker-compose.prod.yml:**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticketero-db
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backup:/backup
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ticketero"]
      interval: 30s
      timeout: 10s
      retries: 3

  api:
    image: ticketero:latest
    container_name: ticketero-api
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/ticketero
      DATABASE_USERNAME: ${DB_USER}
      DATABASE_PASSWORD: ${DB_PASSWORD}
      TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
      JAVA_OPTS: "-Xmx2g -Xms1g -XX:+UseG1GC"
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    volumes:
      - ./logs:/app/logs

  nginx:
    image: nginx:alpine
    container_name: ticketero-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - api
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
```

**Archivo .env.prod:**
```env
# Database
DB_USER=ticketero_user
DB_PASSWORD=super_secure_password_2024!
DATABASE_URL=jdbc:postgresql://postgres:5432/ticketero

# Telegram
TELEGRAM_BOT_TOKEN=your_production_bot_token_here

# Spring
SPRING_PROFILES_ACTIVE=prod

# JVM
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_TICKETERO=INFO
```

### 2. Configurar Nginx

**nginx/nginx.conf:**
```nginx
events {
    worker_connections 1024;
}

http {
    upstream ticketero_backend {
        server api:8080;
    }

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/m;
    limit_req_zone $binary_remote_addr zone=admin:10m rate=100r/m;

    server {
        listen 80;
        server_name your-domain.com;
        
        # Redirect HTTP to HTTPS
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name your-domain.com;

        # SSL Configuration
        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
        ssl_prefer_server_ciphers off;

        # Security Headers
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";
        add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";

        # API Routes
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://ticketero_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
        }

        # Admin Routes
        location /api/admin/ {
            limit_req zone=admin burst=50 nodelay;
            proxy_pass http://ticketero_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Dashboard
        location /dashboard.html {
            proxy_pass http://ticketero_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Health Check
        location /actuator/health {
            proxy_pass http://ticketero_backend;
            access_log off;
        }

        # Static files caching
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            proxy_pass http://ticketero_backend;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

### 3. Build y Deploy

**Script de Deploy (deploy-prod.sh):**
```bash
#!/bin/bash

set -e

echo "🚀 Iniciando deployment de producción..."

# Verificar prerrequisitos
if [ ! -f ".env.prod" ]; then
    echo "❌ Error: Archivo .env.prod no encontrado"
    exit 1
fi

if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
    echo "❌ Error: TELEGRAM_BOT_TOKEN no configurado"
    exit 1
fi

# Build de la aplicación
echo "📦 Building aplicación..."
mvn clean package -DskipTests -Pprod

# Build de imagen Docker
echo "🐳 Building imagen Docker..."
docker build -t ticketero:latest .

# Backup de base de datos (si existe)
if docker ps | grep -q ticketero-db; then
    echo "💾 Creando backup de base de datos..."
    ./backup.sh pre-deploy
fi

# Deploy con Docker Compose
echo "🚀 Desplegando servicios..."
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verificar health checks
echo "🔍 Verificando servicios..."
sleep 30

if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Aplicación desplegada exitosamente"
    echo "🌐 Disponible en: https://your-domain.com"
else
    echo "❌ Error: Aplicación no responde"
    docker-compose -f docker-compose.prod.yml logs api
    exit 1
fi

echo "🎉 Deployment completado!"
```

**Script de Deploy Windows (deploy-prod.bat):**
```batch
@echo off
echo 🚀 Iniciando deployment de producción...

if not exist ".env.prod" (
    echo ❌ Error: Archivo .env.prod no encontrado
    exit /b 1
)

echo 📦 Building aplicación...
mvn clean package -DskipTests -Pprod

echo 🐳 Building imagen Docker...
docker build -t ticketero:latest .

echo 🚀 Desplegando servicios...
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

echo 🔍 Verificando servicios...
timeout /t 30

curl -f http://localhost:8080/actuator/health
if %errorlevel% equ 0 (
    echo ✅ Aplicación desplegada exitosamente
) else (
    echo ❌ Error: Aplicación no responde
    docker-compose -f docker-compose.prod.yml logs api
    exit /b 1
)

echo 🎉 Deployment completado!
```

---

## 🖥️ Deployment Manual (Sin Docker)

### 1. Preparar Servidor

```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Java 21
sudo apt install openjdk-21-jdk -y

# Verificar instalación
java -version

# Instalar PostgreSQL
sudo apt install postgresql postgresql-contrib -y

# Configurar PostgreSQL
sudo -u postgres createuser --interactive ticketero_user
sudo -u postgres createdb ticketero
sudo -u postgres psql -c "ALTER USER ticketero_user PASSWORD 'secure_password';"
```

### 2. Configurar Base de Datos

```sql
-- Conectar como postgres
sudo -u postgres psql

-- Crear usuario y base de datos
CREATE USER ticketero_user WITH PASSWORD 'secure_password';
CREATE DATABASE ticketero OWNER ticketero_user;
GRANT ALL PRIVILEGES ON DATABASE ticketero TO ticketero_user;

-- Configurar conexiones
-- Editar /etc/postgresql/16/main/pg_hba.conf
-- Agregar: local   ticketero   ticketero_user   md5

-- Reiniciar PostgreSQL
sudo systemctl restart postgresql
```

### 3. Deploy de Aplicación

```bash
# Crear usuario para la aplicación
sudo useradd -m -s /bin/bash ticketero

# Crear directorios
sudo mkdir -p /opt/ticketero/{app,logs,config}
sudo chown -R ticketero:ticketero /opt/ticketero

# Copiar JAR
sudo cp target/ticketero-*.jar /opt/ticketero/app/ticketero.jar

# Crear archivo de configuración
sudo tee /opt/ticketero/config/application-prod.yml > /dev/null <<EOF
server:
  port: 8080
  
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://localhost:5432/ticketero
    username: ticketero_user
    password: secure_password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
telegram:
  bot:
    token: \${TELEGRAM_BOT_TOKEN}
    
logging:
  level:
    root: INFO
    com.ticketero: INFO
  file:
    name: /opt/ticketero/logs/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
EOF
```

### 4. Crear Servicio Systemd

```bash
sudo tee /etc/systemd/system/ticketero.service > /dev/null <<EOF
[Unit]
Description=Sistema Ticketero
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=ticketero
Group=ticketero
WorkingDirectory=/opt/ticketero/app
ExecStart=/usr/bin/java -Xmx2g -Xms1g -XX:+UseG1GC -jar ticketero.jar --spring.config.location=/opt/ticketero/config/application-prod.yml
Environment=TELEGRAM_BOT_TOKEN=your_bot_token_here
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# Habilitar y iniciar servicio
sudo systemctl daemon-reload
sudo systemctl enable ticketero
sudo systemctl start ticketero

# Verificar estado
sudo systemctl status ticketero
```

---

## 🔒 Configuración SSL/TLS

### 1. Obtener Certificado (Let's Encrypt)

```bash
# Instalar Certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtener certificado
sudo certbot --nginx -d your-domain.com

# Verificar renovación automática
sudo certbot renew --dry-run
```

### 2. Configuración Manual SSL

```bash
# Generar certificado auto-firmado (desarrollo)
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/key.pem \
  -out /etc/nginx/ssl/cert.pem \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=your-domain.com"
```

---

## 📊 Monitoreo en Producción

### 1. Configurar Prometheus

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'ticketero'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
```

### 2. Configurar Grafana

```bash
# Instalar Grafana
sudo apt-get install -y software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -
sudo apt-get update
sudo apt-get install grafana

# Iniciar Grafana
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

### 3. Dashboard de Monitoreo

**docker-compose.monitoring.yml:**
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: ticketero-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'

  grafana:
    image: grafana/grafana:latest
    container_name: ticketero-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

volumes:
  prometheus_data:
  grafana_data:
```

---

## 💾 Backup y Recovery

### 1. Script de Backup Automático

**backup.sh:**
```bash
#!/bin/bash

BACKUP_DIR="/opt/ticketero/backup"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="ticketero"
DB_USER="ticketero_user"

# Crear directorio de backup
mkdir -p $BACKUP_DIR

# Backup de base de datos
echo "📦 Creando backup de base de datos..."
pg_dump -h localhost -U $DB_USER -d $DB_NAME > $BACKUP_DIR/ticketero_db_$DATE.sql

# Backup de logs
echo "📦 Creando backup de logs..."
tar -czf $BACKUP_DIR/logs_$DATE.tar.gz /opt/ticketero/logs/

# Backup de configuración
echo "📦 Creando backup de configuración..."
tar -czf $BACKUP_DIR/config_$DATE.tar.gz /opt/ticketero/config/

# Limpiar backups antiguos (mantener últimos 7 días)
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "✅ Backup completado: $BACKUP_DIR"
```

### 2. Configurar Cron para Backups

```bash
# Editar crontab
sudo crontab -e

# Agregar backup diario a las 2 AM
0 2 * * * /opt/ticketero/backup.sh >> /opt/ticketero/logs/backup.log 2>&1

# Backup cada 6 horas
0 */6 * * * /opt/ticketero/backup.sh quick >> /opt/ticketero/logs/backup.log 2>&1
```

### 3. Procedimiento de Recovery

```bash
#!/bin/bash
# restore.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Uso: ./restore.sh backup_file.sql"
    exit 1
fi

echo "⚠️  Restaurando base de datos desde: $BACKUP_FILE"
echo "Esto eliminará todos los datos actuales. ¿Continuar? (y/N)"
read -r response

if [[ "$response" =~ ^[Yy]$ ]]; then
    # Detener aplicación
    sudo systemctl stop ticketero
    
    # Restaurar base de datos
    dropdb -U ticketero_user ticketero
    createdb -U ticketero_user ticketero
    psql -U ticketero_user -d ticketero < $BACKUP_FILE
    
    # Reiniciar aplicación
    sudo systemctl start ticketero
    
    echo "✅ Restauración completada"
else
    echo "❌ Restauración cancelada"
fi
```

---

## 🔧 Troubleshooting de Deployment

### Problemas Comunes

**1. Aplicación no inicia:**
```bash
# Verificar logs
sudo journalctl -u ticketero -f

# Verificar configuración
java -jar ticketero.jar --spring.config.location=application-prod.yml --debug
```

**2. Error de conexión a base de datos:**
```bash
# Verificar PostgreSQL
sudo systemctl status postgresql
sudo -u postgres psql -c "SELECT version();"

# Verificar conectividad
telnet localhost 5432
```

**3. Error de Telegram:**
```bash
# Verificar token
curl "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/getMe"

# Verificar conectividad
curl -I https://api.telegram.org
```

**4. Problemas de memoria:**
```bash
# Monitorear memoria
free -h
top -p $(pgrep java)

# Ajustar JVM
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"
```

### Comandos Útiles

```bash
# Ver logs en tiempo real
sudo journalctl -u ticketero -f

# Reiniciar aplicación
sudo systemctl restart ticketero

# Verificar puertos
sudo netstat -tlnp | grep :8080

# Verificar procesos
ps aux | grep java

# Verificar espacio en disco
df -h

# Verificar memoria
free -h

# Test de conectividad
curl -f http://localhost:8080/actuator/health
```

---

## 📋 Checklist de Deployment

### Pre-deployment
- [ ] Servidor configurado con requisitos mínimos
- [ ] Java 21 instalado y configurado
- [ ] PostgreSQL 16+ instalado y configurado
- [ ] Variables de entorno configuradas
- [ ] Certificados SSL obtenidos
- [ ] Backup de datos existentes (si aplica)

### Deployment
- [ ] Aplicación compilada exitosamente
- [ ] Imagen Docker creada (si aplica)
- [ ] Base de datos migrada
- [ ] Servicios iniciados correctamente
- [ ] Health checks pasando
- [ ] Nginx configurado y funcionando

### Post-deployment
- [ ] Monitoreo configurado
- [ ] Logs funcionando correctamente
- [ ] Backups automáticos configurados
- [ ] Alertas configuradas
- [ ] Documentación actualizada
- [ ] Equipo notificado del deployment

---

**Versión:** 1.0  
**Última actualización:** Diciembre 2024  
**DevOps:** Sistema Ticketero Team