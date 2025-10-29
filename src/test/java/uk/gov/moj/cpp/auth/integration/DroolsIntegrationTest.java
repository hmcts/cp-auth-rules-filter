package uk.gov.moj.cpp.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.moj.cpp.auth.drools.AuthAction;
import uk.gov.moj.cpp.auth.drools.DroolsAuthEngine;
import uk.gov.moj.cpp.auth.http.AuthPrincipal;
import uk.gov.moj.cpp.auth.drools.providers.UserAndGroupProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class DroolsIntegrationTest {

    @Autowired
    DroolsAuthEngine droolsAuthEngine;

    @SneakyThrows
    @Test
    void drools_config_should_be_wired_ok() {
        ObjectMapper objectMapper = new ObjectMapper();
        final String actionJson = "{\"name\":\"GET /api/hello\",\"attributes\":{\"method\":\"GET\",\"path\":\"/api/hello\"}}";
        final AuthAction authAction = objectMapper.readValue(actionJson, AuthAction.class);

        final String authPrincipalJson = "{\"userId\":\"ed00e4e3-edda-489a-a41a-86b02e66f412\",\"firstName\":null,\"lastName\":null,\"email\":null,\"groups\":[\"Legal Advisers\",\"Prosecuting Authority Access\",\"Court Administrators\"]}";
        AuthPrincipal authPrincipal = objectMapper.readValue(authPrincipalJson, AuthPrincipal.class);

        UserAndGroupProviderImpl userAndGroupProvider = new UserAndGroupProviderImpl(authPrincipal);
        log.info("Running drools evaluate");
        boolean response = droolsAuthEngine.evaluate(authAction, userAndGroupProvider);
        assertThat(response).isTrue();
    }
}