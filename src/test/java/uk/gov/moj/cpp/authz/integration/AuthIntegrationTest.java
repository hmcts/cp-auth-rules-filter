package uk.gov.moj.cpp.authz.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@Slf4j
@Disabled // Needs some work
class AuthIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Captor
    ArgumentCaptor<String> stringCaptor;

    @Test
    void root_endpoint_should_be_authorised() throws Exception {
        mockMvc
                .perform(
                        post("/")
                                .header("test-header", "some-value")
                                .content("json body"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Hello"));
    }
}