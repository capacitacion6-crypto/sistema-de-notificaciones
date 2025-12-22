# 🧪 Testing Strategy - Sistema Ticketero

## 🎯 Filosofía de Testing

### Pirámide de Testing
```
                    🔺
                   /E2E\     ← Pocos, lentos, costosos
                  /─────\
                 /  API  \    ← Algunos, medianos
                /─────────\
               / UNIT TESTS \  ← Muchos, rápidos, baratos
              /─────────────\
```

**Distribución Objetivo:**
- **70%** Unit Tests (rápidos, aislados)
- **20%** Integration Tests (componentes)
- **10%** End-to-End Tests (flujo completo)

### Principios
- ✅ **Fast**: Tests deben ejecutarse rápidamente
- ✅ **Independent**: Tests no deben depender entre sí
- ✅ **Repeatable**: Mismos resultados en cualquier ambiente
- ✅ **Self-Validating**: Pass/Fail claro, sin interpretación
- ✅ **Timely**: Escritos antes o junto con el código

## 🏗️ Arquitectura de Testing

### Stack Tecnológico
```yaml
Testing Framework: JUnit 5
Mocking: Mockito
Assertions: AssertJ
Test Containers: PostgreSQL, WireMock
Spring Testing: @SpringBootTest, @WebMvcTest
Coverage: JaCoCo
Performance: JMeter (opcional)
```

### Estructura de Directorios
```
src/test/java/
├── unit/                    # Tests unitarios
│   ├── service/
│   ├── controller/
│   └── repository/
├── integration/             # Tests de integración
│   ├── api/
│   ├── database/
│   └── telegram/
├── e2e/                     # Tests end-to-end
│   └── scenarios/
└── fixtures/                # Datos de prueba
    ├── TestDataBuilder.java
    └── MockResponses.java
```

## 🔬 Unit Testing

### 1. Service Layer Tests

**Ejemplo: TicketServiceTest**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Ticket Service Tests")
class TicketServiceTest {
    
    @Mock
    private TicketRepository ticketRepository;
    
    @Mock
    private TelegramService telegramService;
    
    @Mock
    private AsignacionService asignacionService;
    
    @InjectMocks
    private TicketService ticketService;
    
    @Nested
    @DisplayName("Create Ticket")
    class CreateTicketTests {
        
        @Test
        @DisplayName("Should create ticket successfully")
        void shouldCreateTicketSuccessfully() {
            // Given
            TicketRequest request = TicketRequest.builder()
                .tipoServicio(TipoServicio.CAJA)
                .telegramChatId("1234567890")
                .build();
            
            Ticket savedTicket = createTestTicket();
            when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
            when(asignacionService.calcularPosicion(any())).thenReturn(3);
            when(asignacionService.calcularTiempoEstimado(anyInt())).thenReturn(15);
            
            // When
            TicketResponse response = ticketService.create(request);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.numero()).startsWith("C");
            assertThat(response.tipoServicio()).isEqualTo(TipoServicio.CAJA);
            assertThat(response.estado()).isEqualTo(TicketEstado.ESPERANDO);
            assertThat(response.posicionEnCola()).isEqualTo(3);
            
            verify(ticketRepository).save(any(Ticket.class));
            verify(telegramService).sendTicketConfirmation(any(), eq(3), eq(15));
        }
        
        @Test
        @DisplayName("Should throw exception for invalid chat ID")
        void shouldThrowExceptionForInvalidChatId() {
            // Given
            TicketRequest request = TicketRequest.builder()
                .tipoServicio(TipoServicio.CAJA)
                .telegramChatId("invalid")
                .build();
            
            // When & Then
            assertThatThrownBy(() -> ticketService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid chat ID format");
        }
    }
    
    @Nested
    @DisplayName("Find Ticket")
    class FindTicketTests {
        
        @Test
        @DisplayName("Should find ticket by UUID")
        void shouldFindTicketByUuid() {
            // Given
            UUID uuid = UUID.randomUUID();
            Ticket ticket = createTestTicket();
            when(ticketRepository.findByUuid(uuid)).thenReturn(Optional.of(ticket));
            
            // When
            Optional<TicketResponse> response = ticketService.findByUuid(uuid);
            
            // Then
            assertThat(response).isPresent();
            assertThat(response.get().uuid()).isEqualTo(uuid);
        }
        
        @Test
        @DisplayName("Should return empty for non-existent UUID")
        void shouldReturnEmptyForNonExistentUuid() {
            // Given
            UUID uuid = UUID.randomUUID();
            when(ticketRepository.findByUuid(uuid)).thenReturn(Optional.empty());
            
            // When
            Optional<TicketResponse> response = ticketService.findByUuid(uuid);
            
            // Then
            assertThat(response).isEmpty();
        }
    }
    
    private Ticket createTestTicket() {
        return Ticket.builder()
            .id(1L)
            .numero("C001234")
            .uuid(UUID.randomUUID())
            .tipoServicio(TipoServicio.CAJA)
            .estado(TicketEstado.ESPERANDO)
            .telegramChatId("1234567890")
            .createdAt(LocalDateTime.now())
            .build();
    }
}
```

### 2. Controller Layer Tests

**Ejemplo: TicketControllerTest**
```java
@WebMvcTest(TicketController.class)
@DisplayName("Ticket Controller Tests")
class TicketControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TicketService ticketService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("POST /api/tickets - Should create ticket")
    void shouldCreateTicket() throws Exception {
        // Given
        TicketRequest request = new TicketRequest(TipoServicio.CAJA, "1234567890");
        TicketResponse response = createTicketResponse();
        
        when(ticketService.create(any(TicketRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C001234"))
                .andExpect(jsonPath("$.tipoServicio").value("CAJA"))
                .andExpect(jsonPath("$.estado").value("ESPERANDO"))
                .andExpect(jsonPath("$.posicionEnCola").value(3));
        
        verify(ticketService).create(any(TicketRequest.class));
    }
    
    @Test
    @DisplayName("POST /api/tickets - Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given
        TicketRequest invalidRequest = new TicketRequest(null, "invalid");
        
        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }
    
    @Test
    @DisplayName("GET /api/tickets/{uuid} - Should return ticket")
    void shouldReturnTicket() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        TicketResponse response = createTicketResponse();
        
        when(ticketService.findByUuid(uuid)).thenReturn(Optional.of(response));
        
        // When & Then
        mockMvc.perform(get("/api/tickets/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("C001234"));
    }
    
    @Test
    @DisplayName("GET /api/tickets/{uuid} - Should return 404 for non-existent ticket")
    void shouldReturn404ForNonExistentTicket() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        when(ticketService.findByUuid(uuid)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/tickets/{uuid}", uuid))
                .andExpect(status().isNotFound());
    }
    
    private TicketResponse createTicketResponse() {
        return new TicketResponse(
            1L,
            "C001234",
            UUID.randomUUID(),
            TipoServicio.CAJA,
            TicketEstado.ESPERANDO,
            3,
            15,
            LocalDateTime.now(),
            null,
            "1234567890"
        );
    }
}
```

### 3. Repository Layer Tests

**Ejemplo: TicketRepositoryTest**
```java
@DataJpaTest
@DisplayName("Ticket Repository Tests")
class TicketRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Test
    @DisplayName("Should find tickets by estado")
    void shouldFindTicketsByEstado() {
        // Given
        Ticket ticket1 = createAndPersistTicket(TicketEstado.ESPERANDO);
        Ticket ticket2 = createAndPersistTicket(TicketEstado.EN_PROGRESO);
        Ticket ticket3 = createAndPersistTicket(TicketEstado.ESPERANDO);
        
        // When
        List<Ticket> esperandoTickets = ticketRepository
            .findByEstadoOrderByCreatedAtAsc(TicketEstado.ESPERANDO);
        
        // Then
        assertThat(esperandoTickets).hasSize(2);
        assertThat(esperandoTickets).extracting(Ticket::getId)
            .containsExactly(ticket1.getId(), ticket3.getId());
    }
    
    @Test
    @DisplayName("Should find tickets by tipo servicio and estado")
    void shouldFindTicketsByTipoServicioAndEstado() {
        // Given
        createAndPersistTicket(TipoServicio.CAJA, TicketEstado.ESPERANDO);
        createAndPersistTicket(TipoServicio.PERSONAL_BANKER, TicketEstado.ESPERANDO);
        createAndPersistTicket(TipoServicio.CAJA, TicketEstado.EN_PROGRESO);
        
        // When
        List<Ticket> cajaEsperando = ticketRepository
            .findByTipoServicioAndEstadoOrderByCreatedAtAsc(
                TipoServicio.CAJA, TicketEstado.ESPERANDO);
        
        // Then
        assertThat(cajaEsperando).hasSize(1);
        assertThat(cajaEsperando.get(0).getTipoServicio()).isEqualTo(TipoServicio.CAJA);
    }
    
    @Test
    @DisplayName("Should count tickets by estado")
    void shouldCountTicketsByEstado() {
        // Given
        createAndPersistTicket(TicketEstado.ESPERANDO);
        createAndPersistTicket(TicketEstado.ESPERANDO);
        createAndPersistTicket(TicketEstado.COMPLETADO);
        
        // When
        long count = ticketRepository.countByEstado(TicketEstado.ESPERANDO);
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    private Ticket createAndPersistTicket(TicketEstado estado) {
        return createAndPersistTicket(TipoServicio.CAJA, estado);
    }
    
    private Ticket createAndPersistTicket(TipoServicio tipo, TicketEstado estado) {
        Ticket ticket = Ticket.builder()
            .numero("C" + System.currentTimeMillis())
            .uuid(UUID.randomUUID())
            .tipoServicio(tipo)
            .estado(estado)
            .telegramChatId("1234567890")
            .createdAt(LocalDateTime.now())
            .build();
        
        return entityManager.persistAndFlush(ticket);
    }
}
```

## 🔗 Integration Testing

### 1. API Integration Tests

**Base Class:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketero_test")
            .withUsername("test_user")
            .withPassword("test_password");
    
    @Container
    static WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMapping("telegram", BaseIntegrationTest.class, "telegram-mappings.json");
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected TicketRepository ticketRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("telegram.bot.api-url", () -> wireMock.getBaseUrl() + "/bot");
        registry.add("telegram.bot.token", () -> "test_token");
    }
    
    @BeforeEach
    void setUp() {
        // Limpiar datos entre tests
        ticketRepository.deleteAll();
    }
}
```

**Test de Flujo Completo:**
```java
@DisplayName("Ticket Flow Integration Tests")
class TicketFlowIntegrationTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should create ticket and send Telegram notification")
    void shouldCreateTicketAndSendNotification() {
        // Given
        TicketRequest request = new TicketRequest(TipoServicio.CAJA, "1234567890");
        
        // When
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
            "/api/tickets", request, TicketResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().numero()).startsWith("C");
        
        // Verificar en base de datos
        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getEstado()).isEqualTo(TicketEstado.ESPERANDO);
        
        // Verificar llamada a Telegram (WireMock)
        wireMock.verify(postRequestedFor(urlPathMatching("/bot.*/sendMessage"))
            .withRequestBody(containing("Ticket " + response.getBody().numero())));
    }
    
    @Test
    @DisplayName("Should process ticket assignment flow")
    void shouldProcessTicketAssignmentFlow() {
        // Given - Crear ticket
        TicketRequest request = new TicketRequest(TipoServicio.CAJA, "1234567890");
        ResponseEntity<TicketResponse> createResponse = restTemplate.postForEntity(
            "/api/tickets", request, TicketResponse.class);
        
        Long ticketId = createResponse.getBody().id();
        
        // When - Procesar asignación (simular scheduler)
        restTemplate.postForEntity("/api/admin/tickets/" + ticketId + "/assign", 
            null, Void.class);
        
        // Then - Verificar asignación
        ResponseEntity<TicketResponse> getResponse = restTemplate.getForEntity(
            "/api/tickets/" + createResponse.getBody().uuid(), TicketResponse.class);
        
        assertThat(getResponse.getBody().estado()).isEqualTo(TicketEstado.EN_PROGRESO);
        assertThat(getResponse.getBody().advisorAsignado()).isNotNull();
    }
    
    @Test
    @DisplayName("Should complete full ticket lifecycle")
    void shouldCompleteFullTicketLifecycle() {
        // Given - Crear ticket
        TicketRequest request = new TicketRequest(TipoServicio.CAJA, "1234567890");
        ResponseEntity<TicketResponse> createResponse = restTemplate.postForEntity(
            "/api/tickets", request, TicketResponse.class);
        
        Long ticketId = createResponse.getBody().id();
        
        // When - Asignar ticket
        restTemplate.postForEntity("/api/admin/tickets/" + ticketId + "/assign", 
            null, Void.class);
        
        // And - Completar ticket
        ResponseEntity<TicketResponse> completeResponse = restTemplate.postForEntity(
            "/api/admin/tickets/" + ticketId + "/complete", null, TicketResponse.class);
        
        // Then
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completeResponse.getBody().estado()).isEqualTo(TicketEstado.COMPLETADO);
        assertThat(completeResponse.getBody().completedAt()).isNotNull();
        assertThat(completeResponse.getBody().tiempoAtencionMinutos()).isGreaterThan(0);
        
        // Verificar mensaje de completado enviado
        wireMock.verify(postRequestedFor(urlPathMatching("/bot.*/sendMessage"))
            .withRequestBody(containing("Atención completada")));
    }
}
```

### 2. Database Integration Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Database Integration Tests")
class DatabaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private AdvisorRepository advisorRepository;
    
    @Test
    @DisplayName("Should handle concurrent ticket creation")
    void shouldHandleConcurrentTicketCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<CompletableFuture<Ticket>> futures = new ArrayList<>();
        
        // When - Crear tickets concurrentemente
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            CompletableFuture<Ticket> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Ticket ticket = Ticket.builder()
                        .numero("C" + index)
                        .uuid(UUID.randomUUID())
                        .tipoServicio(TipoServicio.CAJA)
                        .estado(TicketEstado.ESPERANDO)
                        .telegramChatId("123456789" + index)
                        .createdAt(LocalDateTime.now())
                        .build();
                    
                    return ticketRepository.save(ticket);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }
        
        latch.await(10, TimeUnit.SECONDS);
        
        // Then
        List<Ticket> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        assertThat(results).hasSize(threadCount);
        assertThat(ticketRepository.count()).isEqualTo(threadCount);
    }
    
    @Test
    @DisplayName("Should maintain referential integrity")
    void shouldMaintainReferentialIntegrity() {
        // Given
        Advisor advisor = Advisor.builder()
            .nombre("Test Advisor")
            .tipoServicio(TipoServicio.CAJA)
            .estado(AdvisorEstado.DISPONIBLE)
            .modulo(1)
            .build();
        advisor = advisorRepository.save(advisor);
        
        Ticket ticket = Ticket.builder()
            .numero("C001")
            .uuid(UUID.randomUUID())
            .tipoServicio(TipoServicio.CAJA)
            .estado(TicketEstado.EN_PROGRESO)
            .advisor(advisor)
            .telegramChatId("1234567890")
            .createdAt(LocalDateTime.now())
            .build();
        ticket = ticketRepository.save(ticket);
        
        // When - Eliminar advisor
        advisorRepository.delete(advisor);
        
        // Then - Ticket debe tener advisor_id = null
        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertThat(updatedTicket.getAdvisor()).isNull();
    }
}
```

## 🎭 End-to-End Testing

### 1. Scenario Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@DisplayName("E2E Scenario Tests")
class TicketSystemE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Container
    static GenericContainer<?> app = new GenericContainer<>("ticketero:test")
            .withExposedPorts(8080)
            .dependsOn(postgres);
    
    private WebDriver driver;
    
    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    @DisplayName("Complete user journey - Create ticket to completion")
    void completeUserJourney() {
        // Given - Abrir dashboard
        String baseUrl = "http://localhost:" + app.getMappedPort(8080);
        driver.get(baseUrl + "/dashboard.html");
        
        // When - Crear ticket via API (simular usuario móvil)
        String ticketNumber = createTicketViaAPI(baseUrl);
        
        // Then - Verificar ticket aparece en dashboard
        WebElement ticketElement = driver.findElement(
            By.xpath("//td[contains(text(), '" + ticketNumber + "')]"));
        assertThat(ticketElement.isDisplayed()).isTrue();
        
        // And - Procesar ticket (simular asesor)
        processTicketInDashboard(ticketNumber);
        
        // And - Completar ticket
        completeTicketInDashboard(ticketNumber);
        
        // Then - Verificar ticket completado
        WebElement completedTicket = driver.findElement(
            By.xpath("//td[contains(text(), '" + ticketNumber + "')]/../td[contains(text(), 'COMPLETADO')]"));
        assertThat(completedTicket.isDisplayed()).isTrue();
    }
    
    private String createTicketViaAPI(String baseUrl) {
        // Implementar llamada HTTP para crear ticket
        // Retornar número de ticket creado
        return "C001234";
    }
    
    private void processTicketInDashboard(String ticketNumber) {
        // Implementar interacción con dashboard para procesar ticket
    }
    
    private void completeTicketInDashboard(String ticketNumber) {
        // Implementar interacción con dashboard para completar ticket
    }
}
```

## 🏗️ Test Data Builders

### 1. Builder Pattern para Tests

```java
public class TicketTestDataBuilder {
    
    private Long id = 1L;
    private String numero = "C001234";
    private UUID uuid = UUID.randomUUID();
    private TipoServicio tipoServicio = TipoServicio.CAJA;
    private TicketEstado estado = TicketEstado.ESPERANDO;
    private String telegramChatId = "1234567890";
    private LocalDateTime createdAt = LocalDateTime.now();
    private Advisor advisor;
    
    public static TicketTestDataBuilder aTicket() {
        return new TicketTestDataBuilder();
    }
    
    public TicketTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }
    
    public TicketTestDataBuilder withNumero(String numero) {
        this.numero = numero;
        return this;
    }
    
    public TicketTestDataBuilder withTipoServicio(TipoServicio tipoServicio) {
        this.tipoServicio = tipoServicio;
        return this;
    }
    
    public TicketTestDataBuilder withEstado(TicketEstado estado) {
        this.estado = estado;
        return this;
    }
    
    public TicketTestDataBuilder withAdvisor(Advisor advisor) {
        this.advisor = advisor;
        return this;
    }
    
    public TicketTestDataBuilder inProgress() {
        this.estado = TicketEstado.EN_PROGRESO;
        return this;
    }
    
    public TicketTestDataBuilder completed() {
        this.estado = TicketEstado.COMPLETADO;
        return this;
    }
    
    public Ticket build() {
        return Ticket.builder()
            .id(id)
            .numero(numero)
            .uuid(uuid)
            .tipoServicio(tipoServicio)
            .estado(estado)
            .telegramChatId(telegramChatId)
            .createdAt(createdAt)
            .advisor(advisor)
            .build();
    }
    
    public TicketResponse buildResponse() {
        Ticket ticket = build();
        return new TicketResponse(
            ticket.getId(),
            ticket.getNumero(),
            ticket.getUuid(),
            ticket.getTipoServicio(),
            ticket.getEstado(),
            0, // posicionEnCola
            0, // tiempoEstimadoMinutos
            ticket.getCreatedAt(),
            ticket.getAdvisor() != null ? new AdvisorResponse(ticket.getAdvisor()) : null,
            ticket.getTelegramChatId()
        );
    }
}

// Uso en tests:
// Ticket ticket = aTicket().withTipoServicio(PERSONAL_BANKER).inProgress().build();
// TicketResponse response = aTicket().completed().buildResponse();
```

### 2. Factory Methods

```java
@Component
public class TestDataFactory {
    
    public Ticket createTicket(TipoServicio tipo, TicketEstado estado) {
        return Ticket.builder()
            .numero(generateTicketNumber(tipo))
            .uuid(UUID.randomUUID())
            .tipoServicio(tipo)
            .estado(estado)
            .telegramChatId("1234567890")
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    public Advisor createAdvisor(TipoServicio tipo, AdvisorEstado estado) {
        return Advisor.builder()
            .nombre("Test Advisor")
            .tipoServicio(tipo)
            .estado(estado)
            .modulo(1)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    public List<Ticket> createTicketQueue(TipoServicio tipo, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createTicket(tipo, TicketEstado.ESPERANDO))
            .collect(Collectors.toList());
    }
    
    private String generateTicketNumber(TipoServicio tipo) {
        String prefix = switch (tipo) {
            case CAJA -> "C";
            case PERSONAL_BANKER -> "PB";
            case EMPRESAS -> "E";
            case GERENCIA -> "G";
        };
        return prefix + System.currentTimeMillis();
    }
}
```

## 📊 Coverage y Quality

### 1. JaCoCo Configuration

**pom.xml:**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2. Objetivos de Coverage

| Capa | Objetivo | Crítico |
|------|----------|---------|
| Service | 90%+ | ✅ |
| Controller | 85%+ | ✅ |
| Repository | 80%+ | ⚠️ |
| Util/Helper | 95%+ | ✅ |
| Overall | 85%+ | ✅ |

### 3. Comandos de Testing

```bash
# Ejecutar todos los tests
mvn test

# Tests con coverage
mvn test jacoco:report

# Solo unit tests
mvn test -Dtest="*Test"

# Solo integration tests
mvn test -Dtest="*IntegrationTest"

# Test específico
mvn test -Dtest="TicketServiceTest"

# Tests con perfil específico
mvn test -Dspring.profiles.active=test

# Verificar coverage mínimo
mvn jacoco:check
```

## 🚀 CI/CD Integration

### 1. GitHub Actions

**.github/workflows/test.yml:**
```yaml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_PASSWORD: test_password
          POSTGRES_USER: test_user
          POSTGRES_DB: ticketero_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: mvn test jacoco:report
      env:
        DATABASE_URL: jdbc:postgresql://localhost:5432/ticketero_test
        DATABASE_USERNAME: test_user
        DATABASE_PASSWORD: test_password
        TELEGRAM_BOT_TOKEN: test_token
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
```

### 2. Quality Gates

```yaml
# Agregar al workflow anterior
    - name: SonarCloud Scan
      uses: SonarSource/sonarcloud-github-action@master
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    
    - name: Quality Gate check
      uses: sonarqube-quality-gate-action@master
      timeout-minutes: 5
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

## 📋 Testing Checklist

### Pre-commit
- [ ] Todos los tests pasan localmente
- [ ] Coverage mínimo alcanzado (85%)
- [ ] No tests ignorados sin justificación
- [ ] Test names descriptivos
- [ ] No hardcoded values en tests

### Code Review
- [ ] Tests cubren casos edge
- [ ] Tests son independientes
- [ ] Mocks apropiados (no over-mocking)
- [ ] Assertions claras y específicas
- [ ] Test data builders utilizados

### CI/CD
- [ ] Tests pasan en pipeline
- [ ] Coverage reportado correctamente
- [ ] Quality gates pasando
- [ ] Performance tests (si aplica)
- [ ] Security tests (si aplica)

---

**Versión:** 1.0  
**Testing Framework:** JUnit 5 + Mockito + TestContainers  
**Última actualización:** Diciembre 2024  
**QA Team:** Sistema Ticketero