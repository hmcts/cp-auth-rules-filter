package uk.gov.moj.cpp.auth.integration.testclasses;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class HelloControllerForTest {

    @GetMapping("/api/hello")
    public ResponseEntity<String> helloEndpoint() {
        log.info("/api/hello endpoint hit");
        return ResponseEntity.ok("Hello");
    }
}
