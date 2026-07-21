package uk.gov.moj.cpp.authz.http.config;

import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.AuthzDecider;
import uk.gov.moj.cpp.authz.http.DefaultIdentityToGroupsMapper;
import uk.gov.moj.cpp.authz.http.HttpAuthzFilter;
import uk.gov.moj.cpp.authz.http.IdentityClient;
import uk.gov.moj.cpp.authz.http.IdentityToGroupsMapper;
import uk.gov.moj.cpp.authz.http.PathExclusionChecker;
import uk.gov.moj.cpp.authz.http.SpringTemplatedUrlFallback;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(HttpAuthzProperties.class)
@ConditionalOnProperty(prefix = "authz.http", name = "enabled", havingValue = "true")
public class AuthzAutoConfiguration {

    /**
     * Canonical Spring MVC handler-mapping bean name. Spring does not expose this as a public constant
     * (it is the {@code @Bean} method name in {@code WebMvcConfigurationSupport}), so we name it here.
     */
    static final String MVC_HANDLER_MAPPING_BEAN = "requestMappingHandlerMapping";

    private final HttpAuthzProperties properties;

    public AuthzAutoConfiguration(final HttpAuthzProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    private void onStart() {
        log.info(
                "CPP HTTP Authz starter ACTIVE -> identityUrlTemplate='{}', accept='{}', userIdHeader='{}', actionHeader='{}', drools='{}', reloadOnEachRequest={}, denyWhenNoRules={}, filterOrder={}",
                properties.getIdentityUrlTemplate(),
                properties.getAcceptHeader(),
                properties.getUserIdHeader(),
                properties.getActionHeader(),
                properties.getDroolsClasspathPattern(),
                properties.isReloadOnEachRequest(),
                properties.isDenyWhenNoRules(),
                properties.getFilterOrder()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public IdentityClient identityClient(final HttpAuthzProperties properties) {
        return new IdentityClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean(IdentityToGroupsMapper.class)
    public IdentityToGroupsMapper identityToGroupsMapper(final HttpAuthzProperties properties) {
        return new DefaultIdentityToGroupsMapper(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DroolsAuthzEngine droolsAuthzEngine(final HttpAuthzProperties properties) {
        return new DroolsAuthzEngine(properties);
    }

    /**
     * Resolves the canonical Spring MVC {@code requestMappingHandlerMapping} by name via {@link Qualifier}.
     * This coexists with Spring Boot Actuator's {@code controllerEndpointHandlerMapping} (also a
     * {@link RequestMappingHandlerMapping}); a plain by-type lookup is ambiguous in that common setup and
     * previously failed startup. {@link ObjectProvider#getIfAvailable()} keeps it optional —
     * {@link SpringTemplatedUrlFallback} treats a {@code null} mapping as "templated fallback disabled".
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringTemplatedUrlFallback springTemplatedUrlFallback(
            @Qualifier(MVC_HANDLER_MAPPING_BEAN)
            final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        final RequestMappingHandlerMapping handlerMapping = handlerMappingProvider.getIfAvailable();
        if (handlerMapping == null) {
            log.warn("No '{}' bean available; templated-URL action fallback is DISABLED. Requests without a "
                    + "vendor media type or '{}' header will resolve to their raw path, so authz rules keyed on "
                    + "route templates will not match. This is expected for non-MVC or media-type-only services; "
                    + "otherwise check the Spring MVC handler mapping is present.",
                    MVC_HANDLER_MAPPING_BEAN, properties.getActionHeader());
        }
        return new SpringTemplatedUrlFallback(handlerMapping);
    }

    @Bean
    @ConditionalOnMissingBean
    public PathExclusionChecker pathExclusionChecker(final HttpAuthzProperties properties) {
        return new PathExclusionChecker(properties.getExcludePathPrefixes());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzDecider authzDecider(final HttpAuthzProperties properties,
                                     final IdentityClient identityClient,
                                     final IdentityToGroupsMapper identityToGroupsMapper,
                                     final DroolsAuthzEngine droolsAuthzEngine,
                                     final SpringTemplatedUrlFallback springTemplatedUrlFallback) {
        return new AuthzDecider(properties, identityClient, identityToGroupsMapper, droolsAuthzEngine,
                springTemplatedUrlFallback);
    }

    @Bean
    public FilterRegistrationBean<HttpAuthzFilter> httpAuthzFilterRegistration(
            final HttpAuthzProperties properties,
            final PathExclusionChecker pathExclusionChecker,
            final AuthzDecider authzDecider) {

        final HttpAuthzFilter filter = new HttpAuthzFilter(properties, pathExclusionChecker, authzDecider);
        final FilterRegistrationBean<HttpAuthzFilter> registration = new FilterRegistrationBean<>(filter);
        final int order = properties.getFilterOrder() != null
                ? properties.getFilterOrder()
                : Ordered.HIGHEST_PRECEDENCE + 30;
        registration.setOrder(order);
        registration.addUrlPatterns("/*");
        registration.setName("cppHttpAuthzFilter");
        return registration;
    }
}
