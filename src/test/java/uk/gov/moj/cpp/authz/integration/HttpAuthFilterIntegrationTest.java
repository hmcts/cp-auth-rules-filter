package uk.gov.moj.cpp.authz.integration;

import jakarta.annotation.Resource;
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
import uk.gov.moj.cpp.authz.http.config.HttpAuthHeaderProperties;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.ACTION_HELLO;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_LA_1;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "auth.rules.identityUrlPath=/usersgroups-query-api/{userId}/permissions",
                "auth.rules.excludePathPrefixes=/usersgroups-query-api"})
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class HttpAuthFilterIntegrationTest {
    @Autowired
    HttpAuthHeaderProperties headerProperties;

    @Resource
    private MockMvc mockMvc;

    @Test
    void default_endpoint_should_be_forbidden() throws Exception {
        final String actionHeader = "application/vnd.usersgroups.get-logged-in-user-permissions+json";
        MvcResult result = mockMvc
                .perform(
                        get("/api/hello")
                                .header(headerProperties.getActionHeaderName(), actionHeader)
                                .header(headerProperties.getUserIdHeaderName(), USER_LA_1))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void hello_endpoint_with_no_action_should_be_authorised() throws Exception {
        MvcResult result = mockMvc
                .perform(
                        get("/api/hello")
                                .header(headerProperties.getUserIdHeaderName(), USER_LA_1))
                .andDo(print())
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("Hello");
    }

    @Test
    void hello_endpoint_with_explicit_action_should_be_authorised() throws Exception {
        MvcResult result = mockMvc
                .perform(
                        get("/api/hello")
                                .header(headerProperties.getUserIdHeaderName(), USER_LA_1)
                                .header(headerProperties.getActionHeaderName(), ACTION_HELLO))
                .andDo(print())
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("Hello");
    }
}