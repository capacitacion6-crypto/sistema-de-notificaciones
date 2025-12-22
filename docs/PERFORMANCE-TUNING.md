# ⚡ Performance Tuning - Sistema Ticketero

## 🎯 Objetivos de Performance

### SLA Targets
```
┌─────────────────────────────────────────────────────────────┐
│                    PERFORMANCE TARGETS                      │
├─────────────────────────────────────────────────────────────┤
│ Response Time    │ < 2 segundos (95th percentile)          │
│ Throughput       │ > 100 requests/segundo                  │
│ Availability     │ 99.9% uptime                           │
│ Error Rate       │ < 1% de requests                       │
│ Database         │ < 100ms query time                     │
│ Memory Usage     │ < 80% heap utilization                 │
│ CPU Usage        │ < 70% average                          │
└─────────────────────────────────────────────────────────────┘
```

### Métricas Clave
- **Latencia P50**: < 500ms
- **Latencia P95**: < 2s
- **Latencia P99**: < 5s
- **Tickets/segundo**: > 10
- **Concurrent Users**: > 50

## 🚀 JVM Tuning

### 1. Configuración de Memoria

**Configuración Básica:**
```bash
# Desarrollo (4GB RAM disponible)
export JAVA_OPTS="-Xmx1g -Xms512m"

# Producción (8GB RAM disponible)
export JAVA_OPTS="-Xmx4g -Xms2g"

# Producción High-Load (16GB RAM disponible)
export JAVA_OPTS="-Xmx8g -Xms4g"
```

**Configuración Avanzada:**
```bash
# JVM Flags optimizados para producción
export JAVA_OPTS="
-Xmx4g
-Xms2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-XX:ReservedCodeCacheSize=256m
-XX:InitialCodeCacheSize=64m
-XX:+TieredCompilation
-XX:TieredStopAtLevel=1
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
"
```

### 2. Garbage Collection Tuning

**G1GC (Recomendado para aplicaciones web):**
```bash
export JAVA_OPTS="$JAVA_OPTS
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:G1NewSizePercent=20
-XX:G1MaxNewSizePercent=30
-XX:G1MixedGCCountTarget=8
-XX:G1MixedGCLiveThresholdPercent=85
-XX:+G1UseAdaptiveIHOP
-XX:G1HeapWastePercent=5
"
```

**ZGC (Para aplicaciones con heap muy grande):**
```bash
export JAVA_OPTS="$JAVA_OPTS
-XX:+UnlockExperimentalVMOptions
-XX:+UseZGC
-XX:+UseLargePages
-XX:+UncommitUnusedMemory
"
```

### 3. Monitoring JVM

**JFR (Java Flight Recorder):**
```bash
export JAVA_OPTS="$JAVA_OPTS
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=60s,filename=ticketero-profile.jfr
-XX:FlightRecorderOptions=settings=profile
"

# Analizar con JMC
jmc ticketero-profile.jfr
```

**GC Logging:**
```bash
export JAVA_OPTS="$JAVA_OPTS
-Xlog:gc*:gc.log:time,tags
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M
"
```

## 🗄️ Database Optimization

### 1. Índices Estratégicos

**Análisis de Queries:**
```sql
-- Habilitar pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Queries más lentas
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;

-- Índices no utilizados
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE idx_tup_read = 0;
```

**Índices Optimizados:**
```sql
-- Índices compuestos para queries comunes
CREATE INDEX CONCURRENTLY idx_ticket_estado_tipo_created 
ON ticket(estado, tipo_servicio, created_at);

CREATE INDEX CONCURRENTLY idx_ticket_advisor_estado 
ON ticket(advisor_id, estado) 
WHERE advisor_id IS NOT NULL;

CREATE INDEX CONCURRENTLY idx_advisor_tipo_estado_modulo 
ON advisor(tipo_servicio, estado, modulo);

-- Índices parciales para casos específicos
CREATE INDEX CONCURRENTLY idx_ticket_waiting 
ON ticket(tipo_servicio, created_at) 
WHERE estado = 'ESPERANDO';

CREATE INDEX CONCURRENTLY idx_ticket_active 
ON ticket(advisor_id, updated_at) 
WHERE estado = 'EN_PROGRESO';

-- Índice para búsquedas de texto
CREATE INDEX CONCURRENTLY idx_ticket_numero_gin 
ON ticket USING gin(numero gin_trgm_ops);
```

### 2. Query Optimization

**Optimización de Queries JPA:**
```java
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // ✅ OPTIMIZADO: Query específica con índice
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.estado = :estado 
        AND t.tipoServicio = :tipo 
        ORDER BY t.createdAt ASC
        """)
    List<Ticket> findWaitingTicketsByType(
        @Param("estado") TicketEstado estado,
        @Param("tipo") TipoServicio tipo,
        Pageable pageable
    );
    
    // ✅ OPTIMIZADO: JOIN FETCH para evitar N+1
    @Query("""
        SELECT t FROM Ticket t 
        LEFT JOIN FETCH t.advisor a 
        WHERE t.estado = :estado
        """)
    List<Ticket> findByEstadoWithAdvisor(@Param("estado") TicketEstado estado);
    
    // ✅ OPTIMIZADO: Projection para reducir datos
    @Query("""
        SELECT new com.ticketero.dto.TicketSummary(
            t.id, t.numero, t.estado, t.createdAt
        ) 
        FROM Ticket t 
        WHERE t.createdAt >= :startDate
        """)
    List<TicketSummary> findTicketSummariesSince(@Param("startDate") LocalDateTime startDate);
    
    // ✅ OPTIMIZADO: Count query optimizada
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.estado = :estado AND t.tipoServicio = :tipo")
    long countByEstadoAndTipo(@Param("estado") TicketEstado estado, @Param("tipo") TipoServicio tipo);
}
```

**Batch Operations:**
```java
@Service
@Transactional
public class TicketBatchService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public void batchUpdateTicketStatus(List<Long> ticketIds, TicketEstado newStatus) {
        String jpql = """
            UPDATE Ticket t 
            SET t.estado = :status, t.updatedAt = :now 
            WHERE t.id IN :ids
            """;
        
        entityManager.createQuery(jpql)
            .setParameter("status", newStatus)
            .setParameter("now", LocalDateTime.now())
            .setParameter("ids", ticketIds)
            .executeUpdate();
    }
    
    public void batchInsertTickets(List<Ticket> tickets) {
        int batchSize = 50;
        for (int i = 0; i < tickets.size(); i++) {
            entityManager.persist(tickets.get(i));
            
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
```

### 3. Connection Pool Tuning

**HikariCP Optimización:**
```yaml
spring:
  datasource:
    hikari:
      # Pool size basado en CPU cores
      maximum-pool-size: 20  # 2 * CPU_CORES
      minimum-idle: 5        # 25% of maximum-pool-size
      
      # Timeouts optimizados
      connection-timeout: 30000      # 30 segundos
      idle-timeout: 600000          # 10 minutos
      max-lifetime: 1800000         # 30 minutos
      
      # Validación de conexiones
      validation-timeout: 5000
      connection-test-query: SELECT 1
      
      # Leak detection
      leak-detection-threshold: 60000  # 1 minuto
      
      # Pool name para monitoring
      pool-name: TicketeroHikariCP
      
      # Propiedades específicas de PostgreSQL
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false
```

### 4. Database Configuration

**PostgreSQL Tuning:**
```sql
-- postgresql.conf optimizations

-- Memory settings
shared_buffers = 2GB                    -- 25% of RAM
effective_cache_size = 6GB              -- 75% of RAM
work_mem = 64MB                         -- For sorting/hashing
maintenance_work_mem = 512MB            -- For VACUUM, CREATE INDEX

-- Checkpoint settings
checkpoint_completion_target = 0.9
wal_buffers = 16MB
checkpoint_timeout = 10min
max_wal_size = 2GB
min_wal_size = 1GB

-- Connection settings
max_connections = 100
superuser_reserved_connections = 3

-- Query planner
random_page_cost = 1.1                  -- For SSD
effective_io_concurrency = 200          -- For SSD

-- Logging
log_min_duration_statement = 1000       -- Log slow queries
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

-- Statistics
track_activities = on
track_counts = on
track_io_timing = on
track_functions = pl
```

## 🌐 Application Layer Optimization

### 1. Caching Strategy

**Redis Configuration:**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 8
        min-idle: 2
        max-wait: -1ms
  
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutos
      cache-null-values: false
```

**Cache Implementation:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CachedTicketService {
    
    private final TicketRepository ticketRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "ticket-position", key = "#ticketId")
    public int calculatePosition(Long ticketId) {
        // Cálculo costoso de posición en cola
        return ticketRepository.calculatePositionInQueue(ticketId);
    }
    
    @Cacheable(value = "advisor-stats", key = "#advisorId", unless = "#result == null")
    public AdvisorStats getAdvisorStats(Long advisorId) {
        // Estadísticas del asesor (cálculo costoso)
        return calculateAdvisorStats(advisorId);
    }
    
    @CacheEvict(value = "ticket-position", allEntries = true)
    public void evictPositionCache() {
        log.info("Evicting ticket position cache");
    }
    
    // Cache manual para datos que cambian frecuentemente
    public DashboardStats getDashboardStats() {
        String cacheKey = "dashboard:stats";
        
        DashboardStats cached = (DashboardStats) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        DashboardStats stats = calculateDashboardStats();
        redisTemplate.opsForValue().set(cacheKey, stats, Duration.ofMinutes(2));
        
        return stats;
    }
}
```

### 2. Async Processing

**Async Configuration:**
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Ticketero-Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean("telegramExecutor")
    public Executor telegramExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Telegram-");
        executor.initialize();
        return executor;
    }
}

@Service
@RequiredArgsConstructor
public class AsyncNotificationService {
    
    @Async("telegramExecutor")
    public CompletableFuture<Void> sendNotificationAsync(String chatId, String message) {
        try {
            telegramService.sendMessage(chatId, message);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send async notification", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Async
    public void processTicketBatch(List<Ticket> tickets) {
        tickets.parallelStream()
            .forEach(this::processTicket);
    }
}
```

### 3. HTTP Client Optimization

**RestTemplate Tuning:**
```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        
        // Connection pool
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        // Timeouts
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(5000)
            .setConnectTimeout(10000)
            .setSocketTimeout(30000)
            .build();
        
        // HTTP Client
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
            .build();
        
        factory.setHttpClient(httpClient);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Error handler
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
        
        return restTemplate;
    }
}
```

## 📊 Monitoring Performance

### 1. Performance Metrics

```java
@Component
@RequiredArgsConstructor
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Latency tracking
    public void recordDatabaseQueryTime(String queryType, Duration duration) {
        Timer.builder("database.query.time")
            .tag("query_type", queryType)
            .register(meterRegistry)
            .record(duration);
    }
    
    public void recordCacheHitRate(String cacheName, boolean hit) {
        Counter.builder("cache.requests")
            .tag("cache", cacheName)
            .tag("result", hit ? "hit" : "miss")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordThroughput(String operation) {
        Counter.builder("operations.throughput")
            .tag("operation", operation)
            .register(meterRegistry)
            .increment();
    }
    
    // JVM metrics
    @EventListener
    public void recordGCEvent(GarbageCollectionNotificationInfo gcInfo) {
        Timer.builder("jvm.gc.time")
            .tag("gc_name", gcInfo.getGcName())
            .tag("gc_action", gcInfo.getGcAction())
            .register(meterRegistry)
            .record(gcInfo.getGcInfo().getDuration(), TimeUnit.MILLISECONDS);
    }
}
```

### 2. Performance Testing

**JMeter Test Plan:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="Ticketero Performance Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <hashTree>
      <ThreadGroup testname="Load Test">
        <stringProp name="ThreadGroup.num_threads">50</stringProp>
        <stringProp name="ThreadGroup.ramp_time">30</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      
      <hashTree>
        <HTTPSamplerProxy testname="Create Ticket">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/tickets</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">{"tipoServicio":"CAJA","telegramChatId":"1234567890"}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
        
        <ResponseAssertion testname="Response Assertion">
          <collectionProp name="Asserion.test_strings">
            <stringProp>201</stringProp>
          </collectionProp>
          <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
        </ResponseAssertion>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

**Gatling Test:**
```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TicketeroLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
  
  val createTicketScenario = scenario("Create Ticket")
    .exec(
      http("Create Ticket")
        .post("/api/tickets")
        .body(StringBody("""{"tipoServicio":"CAJA","telegramChatId":"1234567890"}"""))
        .check(status.is(201))
        .check(jsonPath("$.numero").saveAs("ticketNumber"))
    )
    .pause(1)
    .exec(
      http("Get Ticket")
        .get("/api/tickets/${ticketNumber}/position")
        .check(status.is(200))
    )
  
  setUp(
    createTicketScenario.inject(
      rampUsers(100) during (30 seconds),
      constantUsersPerSec(10) during (5 minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(2000),
     global.successfulRequests.percent.gt(99)
   )
}
```

## 🔧 Optimization Techniques

### 1. Lazy Loading Optimization

```java
@Entity
@Table(name = "ticket")
@NamedEntityGraph(
    name = "Ticket.withAdvisor",
    attributeNodes = @NamedAttributeNode("advisor")
)
public class Ticket {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id")
    private Advisor advisor;
    
    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    private List<Mensaje> mensajes = new ArrayList<>();
}

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    @EntityGraph("Ticket.withAdvisor")
    @Query("SELECT t FROM Ticket t WHERE t.estado = :estado")
    List<Ticket> findByEstadoWithAdvisor(@Param("estado") TicketEstado estado);
}
```

### 2. Pagination Optimization

```java
@RestController
@RequiredArgsConstructor
public class TicketController {
    
    @GetMapping("/api/tickets")
    public ResponseEntity<Page<TicketResponse>> getTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) TicketEstado estado) {
        
        // Validar parámetros
        size = Math.min(size, 100); // Máximo 100 elementos por página
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Ticket> tickets;
        if (estado != null) {
            tickets = ticketRepository.findByEstado(estado, pageable);
        } else {
            tickets = ticketRepository.findAll(pageable);
        }
        
        Page<TicketResponse> response = tickets.map(this::toResponse);
        return ResponseEntity.ok(response);
    }
}
```

### 3. Bulk Operations

```java
@Service
@Transactional
public class BulkOperationService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public void bulkUpdateTicketStatus(List<Long> ticketIds, TicketEstado newStatus) {
        String jpql = """
            UPDATE Ticket t 
            SET t.estado = :status, t.updatedAt = CURRENT_TIMESTAMP 
            WHERE t.id IN :ids
            """;
        
        // Procesar en lotes para evitar límites de SQL
        int batchSize = 1000;
        for (int i = 0; i < ticketIds.size(); i += batchSize) {
            List<Long> batch = ticketIds.subList(i, Math.min(i + batchSize, ticketIds.size()));
            
            entityManager.createQuery(jpql)
                .setParameter("status", newStatus)
                .setParameter("ids", batch)
                .executeUpdate();
        }
    }
    
    public void bulkDeleteOldTickets(LocalDateTime cutoffDate) {
        String jpql = """
            DELETE FROM Ticket t 
            WHERE t.estado = 'COMPLETADO' 
            AND t.completedAt < :cutoffDate
            """;
        
        int deletedCount = entityManager.createQuery(jpql)
            .setParameter("cutoffDate", cutoffDate)
            .executeUpdate();
        
        log.info("Deleted {} old tickets", deletedCount);
    }
}
```

## 📋 Performance Checklist

### JVM Optimization
- [ ] Heap size apropiado (25-50% de RAM)
- [ ] G1GC configurado para baja latencia
- [ ] GC logging habilitado
- [ ] JFR profiling configurado
- [ ] String deduplication habilitado

### Database Optimization
- [ ] Índices en columnas de búsqueda frecuente
- [ ] Connection pool optimizado
- [ ] Query analysis con pg_stat_statements
- [ ] Vacuum y analyze programados
- [ ] Slow query logging habilitado

### Application Optimization
- [ ] Caching implementado para datos frecuentes
- [ ] Async processing para operaciones lentas
- [ ] Pagination en endpoints de listado
- [ ] Bulk operations para modificaciones masivas
- [ ] HTTP client pool configurado

### Monitoring
- [ ] Métricas de performance implementadas
- [ ] Alertas para degradación configuradas
- [ ] Load testing automatizado
- [ ] APM tools configurados
- [ ] Performance baselines establecidos

---

**Versión:** 1.0  
**Performance Target:** < 2s response time, > 100 req/s  
**Última actualización:** Diciembre 2024  
**Performance Team:** Sistema Ticketero