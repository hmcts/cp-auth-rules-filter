package uk.gov.moj.cpp.authz.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzHeaderProperties;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "auth.rules.identityUrlPath=/testidentity/logged-in-user/permissions",
                "auth.rules.excludePathPrefixes=/testidentity"})
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@Slf4j
class AuthIntegrationTest {
    @Autowired
    HttpAuthzHeaderProperties headerProperties;

    @Resource
    private MockMvc mockMvc;

    @Captor
    ArgumentCaptor<String> stringCaptor;

    @Test
    void default_endpoint_should_be_forbidden() throws Exception {
        UUID userId = UUID.fromString("b066839e-30bd-42d9-8101-38cf039d673f");
        final String actionHeader = "application/vnd.usersgroups.get-logged-in-user-permissions+json";
        MvcResult result = mockMvc
                .perform(
                        get("/api/hello")
                                .header(headerProperties.getActionHeaderName(), actionHeader)
                                .header(headerProperties.getUserIdHeaderName(), userId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

//    @Test
//    void hello_endpoint_should_be_authorised() throws Exception {
//        UUID userId = UUID.fromString("b066839e-30bd-42d9-8101-38cf039d673f");
//        final String actionHeader = "application/vnd.usersgroups.get-logged-in-user-permissions+json";
//        mockMvc
//                .perform(
//                        get("/api/hello")
//                                .header(headerProperties.getActionHeaderName(), actionHeader)
//                                .header(headerProperties.getUserIdHeaderName(), userId))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string("Hello"));
//    }
}