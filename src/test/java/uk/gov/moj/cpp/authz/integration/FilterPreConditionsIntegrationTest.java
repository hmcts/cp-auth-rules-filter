package uk.gov.moj.cpp.authz.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class FilterPreConditionsIntegrationTest extends BaseHttpAuthzFilterIntegrationTest {

    @Test
    void returns401WhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void optionsRequestPassesThroughWithoutAuthCheck() throws Exception {
        mockMvc.perform(options("/api/hello"))
                .andExpect(status().isOk());
    }
}
