package uk.gov.moj.cpp.authz.integration.testclasses;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class DummyIdentifyController {

    private static final String IDENTITY_PATH = "testidentity/logged-in-user/permissions";
    final String goodResponse = "{\"groups\":[{\"groupId\":\"63cae459-0e51-4d60-bcf8-c5324be50ba4\",\"groupName\":\"Legal Advisers\",\"prosecutingAuthority\":\"ALL\"},{\"groupId\":\"53292fc8-d164-4a6c-8722-cdbc795cf83a\",\"groupName\":\"Court Administrators\",\"prosecutingAuthority\":\"ALL\"}],\"switchableRoles\":[],\"permissions\":[]}";

    @GetMapping(IDENTITY_PATH)
    public ResponseEntity<String> identityEndpoint() throws JsonProcessingException {
        log.info("Identity request hit TEST endpoint;{}", IDENTITY_PATH);
        return ResponseEntity.ok(goodResponse);
    }
}
