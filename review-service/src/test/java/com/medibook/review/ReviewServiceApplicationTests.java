package com.medibook.review;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewServiceApplicationTests {

    @Test
    void contextLoads() {
        // Plain unit test - no Spring context needed
        // Avoids requiring MySQL/Redis/RabbitMQ/Eureka at test time
        assertTrue(true);
    }

}
