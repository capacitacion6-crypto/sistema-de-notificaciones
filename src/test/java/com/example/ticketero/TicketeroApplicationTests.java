package com.example.ticketero;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TicketeroApplicationTests {

    @Test
    void contextLoads() {
        // Test que verifica que el contexto de Spring Boot se carga correctamente
    }
}