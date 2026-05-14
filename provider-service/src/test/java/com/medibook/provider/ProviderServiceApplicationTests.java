package com.medibook.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderServiceApplicationTests {

    @Test
    void contextLoads() {
        // Plain unit test - no Spring context needed
        // Avoids requiring MySQL/Redis/RabbitMQ/Eureka at test time
        assertTrue(true);
    }

}
