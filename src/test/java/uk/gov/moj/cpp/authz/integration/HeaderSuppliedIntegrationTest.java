package uk.gov.moj.cpp.authz.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class HeaderSuppliedIntegrationTest extends BaseHttpAuthzFilterIntegrationTest {

    @Test
    void allowsRequestWhenActionHeaderMatchesRule() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .header(USER_ID_HEADER, TEST_USER)
                        .header(ACTION_HEADER, "allowed-explicit-action"))
                .andExpect(status().isOk());
    }

    @Test
    void deniesRequestWhenActionHeaderHasNoMatchingRule() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .header(USER_ID_HEADER, TEST_USER)
                        .header(ACTION_HEADER, "denied-action"))
                .andExpect(status().isForbidden());
    }
}
