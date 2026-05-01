package uk.gov.moj.cpp.authz.integration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class IntegrationTestController {

    @GetMapping("/api/resource")
    public String get() {
        return "hello from dummy test controller";
    }

    @PostMapping("/api/resource")
    public String post() {
        return "hello from dummy test controller";
    }
}
