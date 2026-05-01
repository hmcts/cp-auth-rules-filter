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

    @Bean
    @ConditionalOnMissingBean
    public SpringTemplatedUrlFallback springTemplatedUrlFallback(
            final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        return new SpringTemplatedUrlFallback(handlerMappingProvider.getIfAvailable());
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
