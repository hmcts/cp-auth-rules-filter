package uk.gov.moj.cpp.authz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Needed to test the audit filter is applied in integration tests
 * Excluded from jarfile in build.gradle
 */
@SpringBootApplication
public class DummySpringApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DummySpringApplication.class, args);
    }
}