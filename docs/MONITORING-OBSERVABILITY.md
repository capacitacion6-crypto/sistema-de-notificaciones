# 📊 Monitoring & Observability - Sistema Ticketero

## 🎯 Estrategia de Observabilidad

### Los Tres Pilares
```
┌─────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY STACK                      │
├─────────────────────────────────────────────────────────────┤
│ 📊 METRICS    │ Prometheus + Grafana                       │
│ 📝 LOGS       │ Structured Logging + ELK (opcional)       │
│ 🔍 TRACES     │ Spring Boot Actuator + Micrometer         │
└─────────────────────────────────────────────────────────────┘
```

### Objetivos de Monitoreo
- **Disponibilidad**: 99.9% uptime
- **Performance**: <2s response time
- **Errores**: <1% error rate
- **Capacidad**: Alertas antes del 80% de recursos

## 🔧 Spring Boot Actuator

### 1. Configuración Básica

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,threaddump,heapdump
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: always
    info:
      enabled: true
    metrics:
      enabled: true
  health:
    diskspace:
      enabled: true
    db:
      enabled: true
  info:
    env:
      enabled: true
    java:
      enabled: true
    os:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      sla:
        http.server.requests: 100ms, 500ms, 1s, 2s
```

### 2. Health Indicators Personalizados

```java
@Component
public class TelegramHealthIndicator implements HealthIndicator {
    
    private final TelegramService telegramService;
    
    @Override
    public Health health() {
        try {
            boolean isHealthy = telegramService.testConnection();
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("telegram", "Connected")
                    .withDetail("api", "https://api.telegram.org")
                    .withDetail("lastCheck", LocalDateTime.now())
                    .build();
            } else {
                return Health.down()
                    .withDetail("telegram", "Connection failed")
                    .withDetail("error", "Unable to reach Telegram API")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("telegram", "Error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final TicketRepository ticketRepository;
    
    @Override
    public Health health() {
        try {
            long ticketCount = ticketRepository.count();
            long activeTickets = ticketRepository.countByEstado(TicketEstado.EN_PROGRESO);
            
            return Health.up()
                .withDetail("database", "Connected")
                .withDetail("totalTickets", ticketCount)
                .withDetail("activeTickets", activeTickets)
                .withDetail("lastCheck", LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

@Component
public class BusinessHealthIndicator implements HealthIndicator {
    
    private final TicketService ticketService;
    private final AdvisorService advisorService;
    
    @Override
    public Health health() {
        try {
            long waitingTickets = ticketService.countByEstado(TicketEstado.ESPERANDO);
            long availableAdvisors = advisorService.countByEstado(AdvisorEstado.DISPONIBLE);
            
            Health.Builder builder = Health.up();
            
            // Alertas de negocio
            if (waitingTickets > 50) {
                builder = Health.down()
                    .withDetail("alert", "Too many waiting tickets");
            } else if (availableAdvisors == 0 && waitingTickets > 0) {
                builder = Health.down()
                    .withDetail("alert", "No advisors available with waiting tickets");
            }
            
            return builder
                .withDetail("waitingTickets", waitingTickets)
                .withDetail("availableAdvisors", availableAdvisors)
                .withDetail("status", "Business metrics OK")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("business", "Error checking business metrics")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 3. Métricas Personalizadas

```java
@Component
@RequiredArgsConstructor
public class TicketMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter ticketsCreated;
    private final Counter ticketsCompleted;
    private final Counter telegramMessagesSent;
    private final Counter telegramMessagesFailed;
    
    // Gauges
    private final AtomicLong waitingTickets = new AtomicLong(0);
    private final AtomicLong activeTickets = new AtomicLong(0);
    private final AtomicLong availableAdvisors = new AtomicLong(0);
    
    // Timers
    private final Timer ticketProcessingTime;
    private final Timer telegramResponseTime;
    
    @PostConstruct
    public void init() {
        // Counters
        this.ticketsCreated = Counter.builder("tickets.created")
            .description("Total tickets created")
            .register(meterRegistry);
            
        this.ticketsCompleted = Counter.builder("tickets.completed")
            .description("Total tickets completed")
            .register(meterRegistry);
            
        this.telegramMessagesSent = Counter.builder("telegram.messages.sent")
            .description("Total Telegram messages sent")
            .register(meterRegistry);
            
        this.telegramMessagesFailed = Counter.builder("telegram.messages.failed")
            .description("Total Telegram messages failed")
            .register(meterRegistry);
        
        // Gauges
        Gauge.builder("tickets.waiting")
            .description("Current waiting tickets")
            .register(meterRegistry, waitingTickets, AtomicLong::get);
            
        Gauge.builder("tickets.active")
            .description("Current active tickets")
            .register(meterRegistry, activeTickets, AtomicLong::get);
            
        Gauge.builder("advisors.available")
            .description("Current available advisors")
            .register(meterRegistry, availableAdvisors, AtomicLong::get);
        
        // Timers
        this.ticketProcessingTime = Timer.builder("tickets.processing.time")
            .description("Time to process a ticket")
            .register(meterRegistry);
            
        this.telegramResponseTime = Timer.builder("telegram.response.time")
            .description("Telegram API response time")
            .register(meterRegistry);
    }
    
    // Counter methods
    public void incrementTicketsCreated(TipoServicio tipo) {
        ticketsCreated.increment(Tags.of("tipo", tipo.name()));
    }
    
    public void incrementTicketsCompleted(TipoServicio tipo, int durationMinutes) {
        ticketsCompleted.increment(Tags.of("tipo", tipo.name(), "duration_range", getDurationRange(durationMinutes)));
    }
    
    public void incrementTelegramMessagesSent(String messageType) {
        telegramMessagesSent.increment(Tags.of("type", messageType));
    }
    
    public void incrementTelegramMessagesFailed(String messageType, String error) {
        telegramMessagesFailed.increment(Tags.of("type", messageType, "error", error));
    }
    
    // Gauge methods
    public void updateWaitingTickets(long count) {
        waitingTickets.set(count);
    }
    
    public void updateActiveTickets(long count) {
        activeTickets.set(count);
    }
    
    public void updateAvailableAdvisors(long count) {
        availableAdvisors.set(count);
    }
    
    // Timer methods
    public Timer.Sample startTicketProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTicketProcessingTime(Timer.Sample sample, TipoServicio tipo) {
        sample.stop(Timer.builder("tickets.processing.time")
            .tag("tipo", tipo.name())
            .register(meterRegistry));
    }
    
    public Timer.Sample startTelegramTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTelegramResponseTime(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("telegram.response.time")
            .tag("operation", operation)
            .register(meterRegistry));
    }
    
    private String getDurationRange(int minutes) {
        if (minutes <= 5) return "0-5min";
        if (minutes <= 10) return "5-10min";
        if (minutes <= 30) return "10-30min";
        return "30min+";
    }
}
```

### 4. Integración en Services

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final TelegramService telegramService;
    private final TicketMetrics ticketMetrics;
    
    public TicketResponse create(TicketRequest request) {
        Timer.Sample sample = ticketMetrics.startTicketProcessingTimer();
        
        try {
            // Lógica de creación
            Ticket ticket = createTicketEntity(request);
            Ticket savedTicket = ticketRepository.save(ticket);
            
            // Enviar notificación
            telegramService.sendTicketConfirmation(savedTicket, position, estimatedTime);
            
            // Métricas
            ticketMetrics.incrementTicketsCreated(request.tipoServicio());
            ticketMetrics.recordTicketProcessingTime(sample, request.tipoServicio());
            
            log.info("Ticket created successfully: {}", savedTicket.getNumero());
            return toResponse(savedTicket);
            
        } catch (Exception e) {
            log.error("Error creating ticket", e);
            throw e;
        }
    }
    
    @Scheduled(fixedDelay = 30000)
    public void updateMetrics() {
        try {
            long waiting = ticketRepository.countByEstado(TicketEstado.ESPERANDO);
            long active = ticketRepository.countByEstado(TicketEstado.EN_PROGRESO);
            
            ticketMetrics.updateWaitingTickets(waiting);
            ticketMetrics.updateActiveTickets(active);
            
        } catch (Exception e) {
            log.error("Error updating metrics", e);
        }
    }
}
```

## 📊 Prometheus Integration

### 1. Configuración Prometheus

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "ticketero_rules.yml"

scrape_configs:
  - job_name: 'ticketero'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
    scrape_timeout: 10s

  - job_name: 'postgres'
    static_configs:
      - targets: ['localhost:9187']
    scrape_interval: 30s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### 2. Alerting Rules

**ticketero_rules.yml:**
```yaml
groups:
  - name: ticketero.rules
    rules:
      # Application Health
      - alert: ApplicationDown
        expr: up{job="ticketero"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Ticketero application is down"
          description: "Ticketero application has been down for more than 1 minute"
      
      # High Response Time
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket{job="ticketero"}[5m])) > 2
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }}s"
      
      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{job="ticketero",status=~"5.."}[5m]) / rate(http_server_requests_total{job="ticketero"}[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"
      
      # Business Metrics
      - alert: TooManyWaitingTickets
        expr: tickets_waiting > 50
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Too many tickets waiting"
          description: "{{ $value }} tickets are waiting in queue"
      
      - alert: NoAvailableAdvisors
        expr: advisors_available == 0 and tickets_waiting > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "No advisors available"
          description: "No advisors available with {{ $labels.tickets_waiting }} tickets waiting"
      
      # Telegram Issues
      - alert: TelegramMessageFailures
        expr: rate(telegram_messages_failed_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Telegram message failures"
          description: "Telegram message failure rate: {{ $value }}/sec"
      
      # Database Issues
      - alert: DatabaseConnectionsHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool usage high"
          description: "Connection pool usage: {{ $value | humanizePercentage }}"
      
      # System Resources
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM memory usage"
          description: "JVM heap usage: {{ $value | humanizePercentage }}"
      
      - alert: HighCPUUsage
        expr: system_cpu_usage > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage: {{ $value | humanizePercentage }}"
```

### 3. Docker Compose Monitoring Stack

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
      - ./monitoring/rules:/etc/prometheus/rules
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: ticketero-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:latest
    container_name: ticketero-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    restart: unless-stopped

  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    container_name: ticketero-postgres-exporter
    ports:
      - "9187:9187"
    environment:
      DATA_SOURCE_NAME: "postgresql://dev_user:dev_password@postgres:5432/ticketero?sslmode=disable"
    depends_on:
      - postgres
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:
```

## 📈 Grafana Dashboards

### 1. Dashboard Principal

**ticketero-dashboard.json:**
```json
{
  "dashboard": {
    "id": null,
    "title": "Sistema Ticketero - Overview",
    "tags": ["ticketero"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Application Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"ticketero\"}",
            "legendFormat": "Application"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Response Time (95th percentile)",
        "type": "stat",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket{job=\"ticketero\"}[5m]))",
            "legendFormat": "95th percentile"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "s",
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 1},
                {"color": "red", "value": 2}
              ]
            }
          }
        }
      },
      {
        "id": 3,
        "title": "Tickets Waiting",
        "type": "stat",
        "targets": [
          {
            "expr": "tickets_waiting",
            "legendFormat": "Waiting"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 20},
                {"color": "red", "value": 50}
              ]
            }
          }
        }
      },
      {
        "id": 4,
        "title": "Available Advisors",
        "type": "stat",
        "targets": [
          {
            "expr": "advisors_available",
            "legendFormat": "Available"
          }
        ]
      },
      {
        "id": 5,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_total{job=\"ticketero\"}[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec"
          }
        ]
      },
      {
        "id": 6,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_total{job=\"ticketero\",status=~\"4..|5..\"}[5m])",
            "legendFormat": "{{status}}"
          }
        ]
      },
      {
        "id": 7,
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{job=\"ticketero\",area=\"heap\"}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{job=\"ticketero\",area=\"heap\"}",
            "legendFormat": "Heap Max"
          }
        ],
        "yAxes": [
          {
            "label": "Bytes",
            "logBase": 1024
          }
        ]
      },
      {
        "id": 8,
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle"
          },
          {
            "expr": "hikaricp_connections_max",
            "legendFormat": "Max"
          }
        ]
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

### 2. Dashboard de Negocio

**business-dashboard.json:**
```json
{
  "dashboard": {
    "title": "Sistema Ticketero - Business Metrics",
    "panels": [
      {
        "title": "Tickets Created Today",
        "type": "stat",
        "targets": [
          {
            "expr": "increase(tickets_created_total[1d])",
            "legendFormat": "Total"
          }
        ]
      },
      {
        "title": "Tickets by Service Type",
        "type": "piechart",
        "targets": [
          {
            "expr": "increase(tickets_created_total[1d])",
            "legendFormat": "{{tipo}}"
          }
        ]
      },
      {
        "title": "Average Processing Time",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(tickets_processing_time_seconds_sum[5m]) / rate(tickets_processing_time_seconds_count[5m])",
            "legendFormat": "{{tipo}}"
          }
        ]
      },
      {
        "title": "Telegram Messages",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(telegram_messages_sent_total[5m])",
            "legendFormat": "Sent - {{type}}"
          },
          {
            "expr": "rate(telegram_messages_failed_total[5m])",
            "legendFormat": "Failed - {{type}}"
          }
        ]
      }
    ]
  }
}
```

## 📝 Structured Logging

### 1. Configuración de Logging

**logback-spring.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- Async Appender -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <!-- Loggers -->
    <logger name="com.ticketero" level="INFO"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>
</configuration>
```

### 2. Structured Logging en Código

```java
@Component
@Slf4j
public class StructuredLogger {
    
    public void logTicketCreated(Ticket ticket) {
        MDC.put("ticketId", ticket.getId().toString());
        MDC.put("ticketNumber", ticket.getNumero());
        MDC.put("serviceType", ticket.getTipoServicio().name());
        MDC.put("chatId", ticket.getTelegramChatId());
        
        log.info("Ticket created successfully");
        
        MDC.clear();
    }
    
    public void logTicketProcessed(Ticket ticket, Advisor advisor, int processingTimeMinutes) {
        MDC.put("ticketId", ticket.getId().toString());
        MDC.put("ticketNumber", ticket.getNumero());
        MDC.put("advisorId", advisor.getId().toString());
        MDC.put("advisorName", advisor.getNombre());
        MDC.put("processingTimeMinutes", String.valueOf(processingTimeMinutes));
        
        log.info("Ticket processed and completed");
        
        MDC.clear();
    }
    
    public void logTelegramError(String chatId, String messageType, String error) {
        MDC.put("chatId", chatId);
        MDC.put("messageType", messageType);
        MDC.put("errorType", "telegram_api_error");
        
        log.error("Failed to send Telegram message: {}", error);
        
        MDC.clear();
    }
    
    public void logBusinessEvent(String eventType, Map<String, Object> data) {
        data.forEach((key, value) -> MDC.put(key, value.toString()));
        MDC.put("eventType", eventType);
        
        log.info("Business event occurred");
        
        MDC.clear();
    }
}
```

## 🚨 Alerting

### 1. AlertManager Configuration

**alertmanager.yml:**
```yaml
global:
  smtp_smarthost: 'smtp.company.com:587'
  smtp_from: 'alerts@company.com'
  smtp_auth_username: 'alerts@company.com'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
    - match:
        severity: warning
      receiver: 'warning-alerts'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://localhost:5001/'

  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@company.com'
        subject: 'CRITICAL: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts-critical'
        title: 'CRITICAL Alert'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

  - name: 'warning-alerts'
    email_configs:
      - to: 'team@company.com'
        subject: 'WARNING: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'instance']
```

### 2. Slack Integration

```java
@Component
@RequiredArgsConstructor
public class SlackNotificationService {
    
    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;
    
    private final RestTemplate restTemplate;
    
    public void sendAlert(String message, String severity) {
        try {
            SlackMessage slackMessage = SlackMessage.builder()
                .text(message)
                .channel("#ticketero-alerts")
                .username("Ticketero Bot")
                .iconEmoji(":warning:")
                .attachments(List.of(
                    SlackAttachment.builder()
                        .color(getColorForSeverity(severity))
                        .title("Sistema Ticketero Alert")
                        .text(message)
                        .timestamp(Instant.now().getEpochSecond())
                        .build()
                ))
                .build();
            
            restTemplate.postForEntity(slackWebhookUrl, slackMessage, String.class);
            
        } catch (Exception e) {
            log.error("Failed to send Slack notification", e);
        }
    }
    
    private String getColorForSeverity(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "danger";
            case "warning" -> "warning";
            case "info" -> "good";
            default -> "#439FE0";
        };
    }
}

@Builder
record SlackMessage(
    String text,
    String channel,
    String username,
    String iconEmoji,
    List<SlackAttachment> attachments
) {}

@Builder
record SlackAttachment(
    String color,
    String title,
    String text,
    long timestamp
) {}
```

## 📊 Performance Monitoring

### 1. APM Integration (Opcional)

```java
@Configuration
public class APMConfiguration {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", "ticketero",
            "environment", getEnvironment(),
            "version", getVersion()
        );
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "unknown");
    }
    
    private String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
}

// Uso en services
@Service
public class TicketService {
    
    @Timed(name = "ticket.creation", description = "Time taken to create a ticket")
    public TicketResponse create(TicketRequest request) {
        // Implementation
    }
    
    @Timed(name = "ticket.processing", description = "Time taken to process a ticket")
    public void processTicket(Long ticketId) {
        // Implementation
    }
}
```

### 2. Custom Performance Metrics

```java
@Component
@RequiredArgsConstructor
public class PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleTicketCreated(TicketCreatedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        // Medir tiempo desde creación hasta primera notificación
        sample.stop(Timer.builder("ticket.notification.time")
            .tag("type", "creation")
            .register(meterRegistry));
    }
    
    @EventListener
    public void handleTicketAssigned(TicketAssignedEvent event) {
        // Medir tiempo en cola
        Duration waitTime = Duration.between(event.getCreatedAt(), event.getAssignedAt());
        
        Timer.builder("ticket.wait.time")
            .tag("service_type", event.getServiceType().name())
            .register(meterRegistry)
            .record(waitTime);
    }
    
    @Scheduled(fixedDelay = 60000)
    public void recordSystemMetrics() {
        // CPU Usage
        double cpuUsage = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getProcessCpuLoad();
        Gauge.builder("system.cpu.usage")
            .register(meterRegistry, () -> cpuUsage);
        
        // Memory Usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        
        Gauge.builder("jvm.memory.heap.usage")
            .register(meterRegistry, () -> (double) heapUsed / heapMax);
    }
}
```

## 📋 Monitoring Checklist

### Setup
- [ ] Spring Boot Actuator configurado
- [ ] Prometheus metrics habilitadas
- [ ] Health indicators personalizados
- [ ] Métricas de negocio implementadas
- [ ] Logging estructurado configurado

### Dashboards
- [ ] Dashboard principal en Grafana
- [ ] Dashboard de métricas de negocio
- [ ] Dashboard de infraestructura
- [ ] Alertas configuradas
- [ ] Notificaciones funcionando

### Alerting
- [ ] Reglas de alertas definidas
- [ ] Umbrales apropiados configurados
- [ ] Canales de notificación configurados
- [ ] Escalation matrix definida
- [ ] Runbooks documentados

### Maintenance
- [ ] Retención de métricas configurada
- [ ] Rotación de logs configurada
- [ ] Backup de configuraciones
- [ ] Documentación actualizada
- [ ] Equipo entrenado

---

**Versión:** 1.0  
**Monitoring Stack:** Prometheus + Grafana + AlertManager  
**Última actualización:** Diciembre 2024  
**DevOps Team:** Sistema Ticketero