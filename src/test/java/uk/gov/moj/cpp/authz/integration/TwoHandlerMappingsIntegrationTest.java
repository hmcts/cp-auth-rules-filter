package uk.gov.moj.cpp.authz.integration;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.authz.http.IdentityClient;
import uk.gov.moj.cpp.authz.http.SpringTemplatedUrlFallback;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Reproduces the Spring Boot Actuator scenario: a second {@link RequestMappingHandlerMapping} bean
 * ({@code controllerEndpointHandlerMapping}) alongside the MVC one. Before the fix, the authz
 * auto-configuration failed to start the context because it resolved the handler mapping by type only.
 */
@SpringBootTest(
        classes = {IntegrationTestApplication.class, TwoHandlerMappingsIntegrationTest.ExtraHandlerMappingConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "authz.http.enabled=true",
            "authz.http.drools-classpath-pattern=classpath:/drool-test/*.drl",
            "authz.http.deny-when-no-rules=true"
        }
)
class TwoHandlerMappingsIntegrationTest {

    @MockitoBean
    IdentityClient identityClient;

    @Autowired
    private ApplicationContext context;

    @Test
    void contextStartsAndFallbackResolvesTheCanonicalMvcHandlerMapping() {
        assertThat(context.getBeansOfType(RequestMappingHandlerMapping.class)).hasSizeGreaterThanOrEqualTo(2);

        final SpringTemplatedUrlFallback fallback = context.getBean(SpringTemplatedUrlFallback.class);
        // @Qualifier narrowed the ambiguous lookup to the canonical MVC bean (not null, not the actuator one).
        final Object resolvedMapping = ReflectionTestUtils.getField(fallback, "handlerMapping");
        assertThat(resolvedMapping).isSameAs(context.getBean("requestMappingHandlerMapping"));
    }

    @TestConfiguration
    static class ExtraHandlerMappingConfig {
        /** Mirrors actuator's controllerEndpointHandlerMapping (a second RequestMappingHandlerMapping bean). */
        @Bean
        RequestMappingHandlerMapping controllerEndpointHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }
    }
}
