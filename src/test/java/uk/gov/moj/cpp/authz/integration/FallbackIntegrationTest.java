package uk.gov.moj.cpp.authz.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class FallbackIntegrationTest extends BaseHttpAuthzFilterIntegrationTest {

    @Test
    void allowsGetRequestWhenMethodPlusPathMatchesRule() throws Exception {
        mockMvc.perform(get("/api/hello")
                        .header(USER_ID_HEADER, TEST_USER))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPostRequestWhenMethodPlusPathMatchesRule() throws Exception {
        mockMvc.perform(post("/api/echo")
                        .header(USER_ID_HEADER, TEST_USER))
                .andExpect(status().isOk());
    }

    @Test
    void deniesRequestWhenComputedMethodPlusPathHasNoMatchingRule() throws Exception {
        mockMvc.perform(get("/api/unknown-path")
                        .header(USER_ID_HEADER, TEST_USER))
                .andExpect(status().isForbidden());
    }
}
