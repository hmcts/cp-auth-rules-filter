package uk.gov.hmcts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// We have auth rules filter package in uk.gov.moj.cpp
// So need to force the moj package into this hmcts application
@SpringBootApplication(scanBasePackages = {
        "uk.gov.hmcts",
        "uk.gov.moj.cpp"
})
@Slf4j
public class DemoApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
