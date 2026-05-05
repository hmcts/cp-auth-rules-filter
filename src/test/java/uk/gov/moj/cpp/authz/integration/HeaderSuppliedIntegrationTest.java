package uk.gov.moj.cpp.authz.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.moj.cpp.authz.testsupport.TestConstants;

import org.junit.jupiter.api.Test;

class HeaderSuppliedIntegrationTest extends BaseHttpAuthzFilterIntegrationTest {

    @Test
    void allowsRequestWhenActionHeaderMatchesRule() throws Exception {
        mockMvc.perform(get("/api/hello")
                        .header(USER_ID_HEADER, TEST_USER)
                        .header(ACTION_HEADER, TestConstants.ACTION_HEARING_GET_DRAFT_RESULT))
                .andExpect(status().isOk());
    }

    @Test
    void deniesRequestWhenActionHeaderHasNoMatchingRule() throws Exception {
        mockMvc.perform(get("/api/hello")
                        .header(USER_ID_HEADER, TEST_USER)
                        .header(ACTION_HEADER, "unknown.action"))
                .andExpect(status().isForbidden());
    }
}
