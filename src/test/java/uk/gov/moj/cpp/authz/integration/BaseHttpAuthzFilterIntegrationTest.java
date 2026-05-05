package uk.gov.moj.cpp.authz.integration;

import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.authz.http.HttpAuthzFilter;
import uk.gov.moj.cpp.authz.http.IdentityClient;
import uk.gov.moj.cpp.authz.http.IdentityResponse;
import uk.gov.moj.cpp.authz.http.dto.UserGroup;
import uk.gov.moj.cpp.authz.testsupport.TestConstants;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
        classes = IntegrationTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "authz.http.enabled=true",
            "authz.http.drools-classpath-pattern=classpath:/drool-test/*.drl",
            "authz.http.reload-on-each-request=false",
            "authz.http.deny-when-no-rules=true",
            "authz.http.user-id-header=CJSCPPUID",
            "authz.http.action-header=CPP-ACTION"
        }
)
abstract class BaseHttpAuthzFilterIntegrationTest {

    static final String USER_ID_HEADER = "CJSCPPUID";
    static final String ACTION_HEADER = "CPP-ACTION";
    static final String TEST_USER = "test-user-123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterRegistrationBean<HttpAuthzFilter> filterRegistrationBean;

    @MockitoBean
    IdentityClient identityClient;

    MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvcAndIdentity() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(filterRegistrationBean.getFilter())
                .build();

        final IdentityResponse identity = new IdentityResponse(
                TEST_USER,
                List.of(new UserGroup(null, TestConstants.GROUP_LA, null)),
                List.of()
        );
        when(identityClient.fetchIdentity(TEST_USER)).thenReturn(identity);
    }
}
