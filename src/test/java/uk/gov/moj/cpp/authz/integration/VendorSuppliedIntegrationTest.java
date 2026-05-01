package uk.gov.moj.cpp.authz.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class VendorSuppliedIntegrationTest extends BaseHttpAuthzFilterIntegrationTest {

    @Test
    void allowsRequestWhenContentTypeVendorTokenMatchesRule() throws Exception {
        mockMvc.perform(post("/api/resource")
                        .header(USER_ID_HEADER, TEST_USER)
                        .contentType("application/vnd.sjp.delete-financial-means+json"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsRequestWhenAcceptVendorTokenMatchesRule() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .header(USER_ID_HEADER, TEST_USER)
                        .header("Accept", "application/vnd.sjp.delete-financial-means+json"))
                .andExpect(status().isOk());
    }

    @Test
    void deniesRequestWhenVendorTokenHasNoMatchingRule() throws Exception {
        mockMvc.perform(post("/api/resource")
                        .header(USER_ID_HEADER, TEST_USER)
                        .contentType("application/vnd.unknown.action+json"))
                .andExpect(status().isForbidden());
    }
}
