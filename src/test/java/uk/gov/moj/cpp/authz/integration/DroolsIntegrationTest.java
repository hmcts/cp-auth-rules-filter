package uk.gov.moj.cpp.authz.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.moj.cpp.authz.drools.DroolsAuthEngine;
import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;
import uk.gov.moj.cpp.authz.http.providers.UserAndGroupProviderImpl;

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
        final Action action = objectMapper.readValue(actionJson, Action.class);

        final String authzPrincipalJson = "{\"userId\":\"ed00e4e3-edda-489a-a41a-86b02e66f412\",\"firstName\":null,\"lastName\":null,\"email\":null,\"groups\":[\"Legal Advisers\",\"Prosecuting Authority Access\",\"Court Administrators\"]}";
        AuthzPrincipal authzPrincipal = objectMapper.readValue(authzPrincipalJson, AuthzPrincipal.class);

        UserAndGroupProviderImpl userAndGroupProvider = new UserAndGroupProviderImpl(authzPrincipal);
        log.info("Running drools evaluate");
        boolean response = droolsAuthEngine.evaluate(userAndGroupProvider, action);
        assertThat(response).isTrue();
    }
}