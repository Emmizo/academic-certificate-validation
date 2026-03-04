// SDLC Phase: Implementation
// Component: Application Bootstrap
// Requirements covered: NFR-06
// Description: Starts the single Spring Boot application instance
package com.certsign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CertSignApplication {
    public static void main(String[] args) {
        SpringApplication.run(CertSignApplication.class, args);
    }
}

