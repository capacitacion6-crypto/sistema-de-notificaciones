# 🔒 Security Guide - Sistema Ticketero

## 🎯 Modelo de Seguridad

### Principios de Seguridad
- **Defense in Depth**: Múltiples capas de seguridad
- **Least Privilege**: Acceso mínimo necesario
- **Fail Secure**: Fallar de manera segura
- **Security by Design**: Seguridad desde el diseño
- **Zero Trust**: No confiar, siempre verificar

### Clasificación de Datos
```
┌─────────────────────────────────────────────────────────────┐
│                    CLASIFICACIÓN DE DATOS                   │
├─────────────────────────────────────────────────────────────┤
│ 🔴 CRÍTICO    │ Tokens, Passwords, Keys                    │
│ 🟡 SENSIBLE   │ Chat IDs, Números de ticket                │
│ 🟢 PÚBLICO    │ Estados, Tipos de servicio                 │
└─────────────────────────────────────────────────────────────┘
```

## 🛡️ Análisis de Amenazas

### OWASP Top 10 - Mitigaciones

**1. Broken Access Control**
- ✅ **Mitigación**: Validación de autorización en cada endpoint
- ✅ **Implementado**: Rate limiting por IP
- 🔄 **Pendiente**: JWT tokens para admin endpoints

**2. Cryptographic Failures**
- ✅ **Mitigación**: HTTPS obligatorio en producción
- ✅ **Implementado**: Variables de entorno para secrets
- ✅ **Implementado**: Hashing de datos sensibles

**3. Injection**
- ✅ **Mitigación**: JPA Query derivadas (type-safe)
- ✅ **Implementado**: `@Param` en queries custom
- ✅ **Implementado**: Validación de inputs con Jakarta

**4. Insecure Design**
- ✅ **Mitigación**: Arquitectura en capas
- ✅ **Implementado**: Separación de responsabilidades
- ✅ **Implementado**: Principio de menor privilegio

**5. Security Misconfiguration**
- ✅ **Mitigación**: Configuración explícita de seguridad
- ⚠️ **Parcial**: Headers de seguridad (implementar más)
- 🔄 **Pendiente**: Hardening de servidor

**6. Vulnerable Components**
- ✅ **Mitigación**: Dependencias actualizadas
- 🔄 **Pendiente**: Escaneo automático de vulnerabilidades
- 🔄 **Pendiente**: SBOM (Software Bill of Materials)

**7. Authentication Failures**
- 🔄 **Pendiente**: Implementar autenticación robusta
- 🔄 **Pendiente**: Multi-factor authentication
- 🔄 **Pendiente**: Session management

**8. Software Integrity Failures**
- ✅ **Mitigación**: Verificación de integridad en CI/CD
- 🔄 **Pendiente**: Firma de artefactos
- 🔄 **Pendiente**: Supply chain security

**9. Logging Failures**
- ✅ **Mitigación**: Logging estructurado
- ✅ **Implementado**: No logging de datos sensibles
- ⚠️ **Parcial**: Monitoreo de eventos de seguridad

**10. Server-Side Request Forgery**
- ✅ **Mitigación**: Validación de URLs externas
- ✅ **Implementado**: Whitelist de dominios Telegram
- ✅ **Implementado**: Timeouts en requests HTTP

## 🔐 Autenticación y Autorización

### Estado Actual (v1.0)
```yaml
Autenticación: ❌ No implementada
Autorización: ❌ Endpoints públicos
Rate Limiting: ✅ Básico por IP
HTTPS: ⚠️ Solo en producción
```

### Roadmap de Seguridad

**Fase 1 - Autenticación Básica**
```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @PostMapping("/tickets/{id}/complete")
    @PreAuthorize("hasAuthority('COMPLETE_TICKET')")
    public ResponseEntity<TicketResponse> completeTicket(@PathVariable Long id) {
        // Implementación
    }
}
```

**Fase 2 - JWT Implementation**
```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    @Value("${security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${security.jwt.expiration:86400}")
    private int jwtExpirationInMs;
    
    public String generateToken(UserPrincipal userPrincipal) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs * 1000L);
        
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
```

**Fase 3 - Role-Based Access Control**
```java
public enum Role {
    ADMIN("ADMIN", Set.of(
        Permission.VIEW_DASHBOARD,
        Permission.MANAGE_ADVISORS,
        Permission.COMPLETE_TICKETS,
        Permission.VIEW_REPORTS
    )),
    
    SUPERVISOR("SUPERVISOR", Set.of(
        Permission.VIEW_DASHBOARD,
        Permission.COMPLETE_TICKETS
    )),
    
    ADVISOR("ADVISOR", Set.of(
        Permission.VIEW_ASSIGNED_TICKETS,
        Permission.COMPLETE_OWN_TICKETS
    ));
    
    private final String name;
    private final Set<Permission> permissions;
}
```

## 🔒 Validación de Entrada

### 1. Input Validation

**DTO Validation:**
```java
public record TicketRequest(
    @NotNull(message = "Tipo de servicio es requerido")
    @Valid
    TipoServicio tipoServicio,
    
    @NotBlank(message = "Chat ID es requerido")
    @Pattern(
        regexp = "^[0-9]{10}$", 
        message = "Chat ID debe tener exactamente 10 dígitos"
    )
    String telegramChatId
) {
    // Validación adicional en constructor compacto
    public TicketRequest {
        if (telegramChatId != null && telegramChatId.startsWith("0")) {
            throw new IllegalArgumentException("Chat ID no puede empezar con 0");
        }
    }
}
```

**Custom Validators:**
```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TelegramChatIdValidator.class)
public @interface ValidTelegramChatId {
    String message() default "Invalid Telegram Chat ID";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class TelegramChatIdValidator implements ConstraintValidator<ValidTelegramChatId, String> {
    
    private static final Pattern CHAT_ID_PATTERN = Pattern.compile("^[1-9][0-9]{9}$");
    
    @Override
    public boolean isValid(String chatId, ConstraintValidatorContext context) {
        if (chatId == null) {
            return false;
        }
        
        // Validar formato
        if (!CHAT_ID_PATTERN.matcher(chatId).matches()) {
            return false;
        }
        
        // Validaciones adicionales de negocio
        return isValidTelegramChatId(chatId);
    }
    
    private boolean isValidTelegramChatId(String chatId) {
        // Implementar validaciones específicas
        return true;
    }
}
```

### 2. SQL Injection Prevention

**Query Derivadas (Recomendado):**
```java
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // ✅ SEGURO: Spring genera query automáticamente
    List<Ticket> findByTelegramChatIdAndEstado(String chatId, TicketEstado estado);
    
    // ✅ SEGURO: Parámetros nombrados
    @Query("SELECT t FROM Ticket t WHERE t.numero = :numero AND t.estado = :estado")
    Optional<Ticket> findByNumeroAndEstado(@Param("numero") String numero, 
                                          @Param("estado") TicketEstado estado);
    
    // ❌ PELIGROSO: Nunca hacer esto
    // @Query(value = "SELECT * FROM ticket WHERE numero = '" + numero + "'", nativeQuery = true)
}
```

**Validación de Parámetros:**
```java
@Service
@RequiredArgsConstructor
@Validated
public class TicketService {
    
    public Optional<TicketResponse> findByNumero(
            @NotBlank @Pattern(regexp = "^[A-Z]{1,3}[0-9]{6}$") String numero) {
        
        // Sanitización adicional
        String sanitizedNumero = numero.trim().toUpperCase();
        
        return ticketRepository.findByNumero(sanitizedNumero)
                .map(this::toResponse);
    }
}
```

## 🌐 Seguridad de Red

### 1. HTTPS Configuration

**Nginx SSL Configuration:**
```nginx
server {
    listen 443 ssl http2;
    server_name ticketero.company.com;
    
    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/ticketero.crt;
    ssl_certificate_key /etc/nginx/ssl/ticketero.key;
    
    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
    
    # SSL Security
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    
    location / {
        proxy_pass http://ticketero_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Security headers for proxied requests
        proxy_hide_header X-Powered-By;
        proxy_hide_header Server;
    }
}
```

### 2. Rate Limiting

**Application Level:**
```java
@Component
@RequiredArgsConstructor
public class RateLimitingService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isAllowed(String clientId, String endpoint, int maxRequests, Duration window) {
        String key = "rate_limit:" + clientId + ":" + endpoint;
        String currentCount = redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            redisTemplate.opsForValue().set(key, "1", window);
            return true;
        }
        
        int count = Integer.parseInt(currentCount);
        if (count >= maxRequests) {
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}

@Component
public class RateLimitingFilter implements Filter {
    
    private final RateLimitingService rateLimitingService;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIp(httpRequest);
        String endpoint = httpRequest.getRequestURI();
        
        // Diferentes límites por endpoint
        RateLimit rateLimit = getRateLimitForEndpoint(endpoint);
        
        if (!rateLimitingService.isAllowed(clientIp, endpoint, 
                rateLimit.maxRequests(), rateLimit.window())) {
            
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private RateLimit getRateLimitForEndpoint(String endpoint) {
        if (endpoint.startsWith("/api/admin")) {
            return new RateLimit(100, Duration.ofMinutes(1)); // Admin: 100/min
        } else if (endpoint.startsWith("/api/tickets")) {
            return new RateLimit(10, Duration.ofMinutes(1));  // Tickets: 10/min
        }
        return new RateLimit(50, Duration.ofMinutes(1));      // Default: 50/min
    }
    
    record RateLimit(int maxRequests, Duration window) {}
}
```

### 3. Firewall Rules

**iptables Configuration:**
```bash
#!/bin/bash
# firewall-rules.sh

# Limpiar reglas existentes
iptables -F
iptables -X
iptables -t nat -F
iptables -t nat -X

# Política por defecto: denegar todo
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# Permitir loopback
iptables -A INPUT -i lo -j ACCEPT

# Permitir conexiones establecidas
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# SSH (cambiar puerto por defecto)
iptables -A INPUT -p tcp --dport 2222 -j ACCEPT

# HTTP/HTTPS
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# PostgreSQL (solo desde localhost)
iptables -A INPUT -p tcp -s 127.0.0.1 --dport 5432 -j ACCEPT

# Rate limiting para HTTP
iptables -A INPUT -p tcp --dport 80 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT

# Protección contra ataques
iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP
iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP
iptables -A INPUT -p tcp --tcp-flags ALL FIN,URG,PSH -j DROP
iptables -A INPUT -p tcp --tcp-flags ALL SYN,RST,ACK,FIN,URG -j DROP

# Guardar reglas
iptables-save > /etc/iptables/rules.v4
```

## 🔐 Gestión de Secretos

### 1. Variables de Entorno

**Producción (.env.prod):**
```env
# Database (usar secretos seguros)
DATABASE_PASSWORD=${DB_PASSWORD_FROM_VAULT}

# Telegram (rotar periódicamente)
TELEGRAM_BOT_TOKEN=${TELEGRAM_TOKEN_FROM_VAULT}

# JWT (generar clave fuerte)
JWT_SECRET=${JWT_SECRET_FROM_VAULT}

# Encryption (para datos sensibles)
ENCRYPTION_KEY=${ENCRYPTION_KEY_FROM_VAULT}
```

**Generación de Secretos:**
```bash
# JWT Secret (256 bits)
openssl rand -base64 32

# Encryption Key (AES-256)
openssl rand -hex 32

# Database Password (fuerte)
openssl rand -base64 24 | tr -d "=+/" | cut -c1-16
```

### 2. Vault Integration (Futuro)

```java
@Configuration
@EnableConfigurationProperties(VaultProperties.class)
public class VaultConfig {
    
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = new VaultEndpoint();
        endpoint.setHost("vault.company.com");
        endpoint.setPort(8200);
        endpoint.setScheme("https");
        
        ClientAuthentication authentication = new TokenAuthentication(getVaultToken());
        
        return new VaultTemplate(endpoint, authentication);
    }
    
    @Bean
    @Primary
    public PropertySource<?> vaultPropertySource(VaultTemplate vaultTemplate) {
        return new VaultPropertySource(vaultTemplate, "secret/ticketero");
    }
}
```

### 3. Encryption at Rest

```java
@Component
public class DataEncryptionService {
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    private final AESUtil aesUtil = new AESUtil();
    
    public String encrypt(String plainText) {
        try {
            return aesUtil.encrypt(plainText, encryptionKey);
        } catch (Exception e) {
            throw new SecurityException("Failed to encrypt data", e);
        }
    }
    
    public String decrypt(String encryptedText) {
        try {
            return aesUtil.decrypt(encryptedText, encryptionKey);
        } catch (Exception e) {
            throw new SecurityException("Failed to decrypt data", e);
        }
    }
}

// Uso en entities sensibles
@Entity
@Table(name = "sensitive_data")
public class SensitiveData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String sensitiveField;
}

@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    private final DataEncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? encryptionService.encrypt(attribute) : null;
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData != null ? encryptionService.decrypt(dbData) : null;
    }
}
```

## 📊 Logging y Auditoría

### 1. Security Logging

```java
@Component
@Slf4j
public class SecurityEventLogger {
    
    private static final String SECURITY_MARKER = "SECURITY";
    
    public void logAuthenticationSuccess(String username, String ip) {
        log.info(SECURITY_MARKER, "Authentication successful - User: {}, IP: {}", username, ip);
    }
    
    public void logAuthenticationFailure(String username, String ip, String reason) {
        log.warn(SECURITY_MARKER, "Authentication failed - User: {}, IP: {}, Reason: {}", 
            username, ip, reason);
    }
    
    public void logUnauthorizedAccess(String endpoint, String ip, String userAgent) {
        log.warn(SECURITY_MARKER, "Unauthorized access attempt - Endpoint: {}, IP: {}, UserAgent: {}", 
            endpoint, ip, userAgent);
    }
    
    public void logRateLimitExceeded(String ip, String endpoint) {
        log.warn(SECURITY_MARKER, "Rate limit exceeded - IP: {}, Endpoint: {}", ip, endpoint);
    }
    
    public void logSuspiciousActivity(String activity, String details) {
        log.error(SECURITY_MARKER, "Suspicious activity detected - Activity: {}, Details: {}", 
            activity, details);
    }
}
```

### 2. Audit Trail

```java
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String entityType;
    
    @Column(nullable = false)
    private String entityId;
    
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, VIEW
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String oldValues;
    
    @Column(columnDefinition = "TEXT")
    private String newValues;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}

@Component
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public void auditCreate(String entityType, String entityId, Object entity, String username, String ip) {
        try {
            AuditLog audit = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action("CREATE")
                .username(username)
                .ipAddress(ip)
                .newValues(objectMapper.writeValueAsString(entity))
                .build();
            
            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }
    
    public void auditUpdate(String entityType, String entityId, Object oldEntity, Object newEntity, 
                           String username, String ip) {
        try {
            AuditLog audit = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action("UPDATE")
                .username(username)
                .ipAddress(ip)
                .oldValues(objectMapper.writeValueAsString(oldEntity))
                .newValues(objectMapper.writeValueAsString(newEntity))
                .build();
            
            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }
}
```

## 🚨 Monitoreo de Seguridad

### 1. Security Metrics

```java
@Component
@RequiredArgsConstructor
public class SecurityMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private final Counter authenticationFailures;
    private final Counter unauthorizedAccess;
    private final Counter rateLimitExceeded;
    private final Timer authenticationTime;
    
    @PostConstruct
    public void init() {
        this.authenticationFailures = Counter.builder("security.authentication.failures")
            .description("Number of authentication failures")
            .register(meterRegistry);
            
        this.unauthorizedAccess = Counter.builder("security.unauthorized.access")
            .description("Number of unauthorized access attempts")
            .register(meterRegistry);
            
        this.rateLimitExceeded = Counter.builder("security.rate.limit.exceeded")
            .description("Number of rate limit violations")
            .register(meterRegistry);
            
        this.authenticationTime = Timer.builder("security.authentication.time")
            .description("Authentication processing time")
            .register(meterRegistry);
    }
    
    public void recordAuthenticationFailure(String reason) {
        authenticationFailures.increment(Tags.of("reason", reason));
    }
    
    public void recordUnauthorizedAccess(String endpoint) {
        unauthorizedAccess.increment(Tags.of("endpoint", endpoint));
    }
    
    public void recordRateLimitExceeded(String endpoint) {
        rateLimitExceeded.increment(Tags.of("endpoint", endpoint));
    }
    
    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(meterRegistry);
    }
}
```

### 2. Alerting Rules

**Prometheus Alerting Rules:**
```yaml
groups:
  - name: security.rules
    rules:
      - alert: HighAuthenticationFailures
        expr: rate(security_authentication_failures_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High authentication failure rate"
          description: "Authentication failure rate is {{ $value }} per second"
      
      - alert: UnauthorizedAccessSpike
        expr: rate(security_unauthorized_access_total[5m]) > 5
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Spike in unauthorized access attempts"
          description: "Unauthorized access rate is {{ $value }} per second"
      
      - alert: RateLimitViolations
        expr: rate(security_rate_limit_exceeded_total[5m]) > 20
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "High rate limit violations"
          description: "Rate limit violations: {{ $value }} per second"
```

## 🔍 Security Testing

### 1. Security Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class SecurityTest {
    
    @Test
    @DisplayName("Should reject invalid chat ID format")
    void shouldRejectInvalidChatIdFormat() {
        // Given
        List<String> invalidChatIds = Arrays.asList(
            "123456789",    // Too short
            "12345678901",  // Too long
            "0123456789",   // Starts with 0
            "123456789a",   // Contains letter
            "123-456-789",  // Contains special chars
            "",             // Empty
            null            // Null
        );
        
        // When & Then
        for (String invalidChatId : invalidChatIds) {
            assertThatThrownBy(() -> new TicketRequest(TipoServicio.CAJA, invalidChatId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid chat ID");
        }
    }
    
    @Test
    @DisplayName("Should sanitize input parameters")
    void shouldSanitizeInputParameters() {
        // Given
        String maliciousInput = "<script>alert('xss')</script>";
        String sqlInjection = "'; DROP TABLE ticket; --";
        
        // When & Then
        assertThatThrownBy(() -> ticketService.findByNumero(maliciousInput))
            .isInstanceOf(ConstraintViolationException.class);
            
        assertThatThrownBy(() -> ticketService.findByNumero(sqlInjection))
            .isInstanceOf(ConstraintViolationException.class);
    }
}
```

### 2. Penetration Testing

**Automated Security Scanning:**
```bash
#!/bin/bash
# security-scan.sh

echo "🔍 Running security scans..."

# OWASP ZAP Baseline Scan
docker run -t owasp/zap2docker-stable zap-baseline.py \
    -t http://localhost:8080 \
    -r zap-report.html

# Nikto Web Scanner
nikto -h http://localhost:8080 -output nikto-report.txt

# SSL Labs Test (production)
if [ "$ENVIRONMENT" = "prod" ]; then
    curl -s "https://api.ssllabs.com/api/v3/analyze?host=ticketero.company.com" \
        | jq '.endpoints[0].grade'
fi

# Dependency Check
mvn org.owasp:dependency-check-maven:check

echo "✅ Security scans completed"
```

## 📋 Security Checklist

### Development
- [ ] Input validation en todos los endpoints
- [ ] Queries parametrizadas (no concatenación)
- [ ] Secrets en variables de entorno
- [ ] Logging sin datos sensibles
- [ ] Error handling que no exponga información

### Deployment
- [ ] HTTPS configurado correctamente
- [ ] Headers de seguridad implementados
- [ ] Rate limiting activado
- [ ] Firewall configurado
- [ ] Certificados SSL válidos

### Operations
- [ ] Monitoreo de eventos de seguridad
- [ ] Alertas configuradas
- [ ] Logs de auditoría funcionando
- [ ] Backups encriptados
- [ ] Rotación de secretos programada

### Compliance
- [ ] Documentación de seguridad actualizada
- [ ] Políticas de acceso definidas
- [ ] Procedimientos de respuesta a incidentes
- [ ] Entrenamiento de seguridad del equipo
- [ ] Revisiones de seguridad periódicas

---

**Versión:** 1.0  
**Security Framework:** Spring Security + Custom  
**Última actualización:** Diciembre 2024  
**Security Team:** Sistema Ticketero