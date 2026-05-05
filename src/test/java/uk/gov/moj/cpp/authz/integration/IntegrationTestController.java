package uk.gov.moj.cpp.authz.integration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class IntegrationTestController {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @PostMapping("/echo")
    public String echo() {
        return "echo";
    }
}
