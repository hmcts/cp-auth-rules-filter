package uk.gov.moj.cpp.auth.integration;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"auth.rules.disabled=true"})
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class FilterDisabledIntegrationTest {

    @Resource
    private MockMvc mockMvc;


    @Test
    void when_disabled_should_allow_unauthorised() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/api/hello"))
                .andDo(print())
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }
}