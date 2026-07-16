package uk.gov.moj.cpp.authz.http.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class AuthzAutoConfigurationTest {

    private final AuthzAutoConfiguration config =
            new AuthzAutoConfiguration(HttpAuthzProperties.builder().build());

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(AuthzAutoConfiguration.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    @SuppressWarnings("unchecked")
    void warnsWhenHandlerMappingIsUnavailable() {
        final ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(config.springTemplatedUrlFallback(provider)).isNotNull();
        assertThat(appender.list)
                .anyMatch(e -> e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains("templated-URL action fallback is DISABLED"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void doesNotWarnWhenHandlerMappingIsAvailable() {
        final ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(new RequestMappingHandlerMapping());

        assertThat(config.springTemplatedUrlFallback(provider)).isNotNull();
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.WARN);
    }

    /**
     * Guards {@link AuthzAutoConfiguration#MVC_HANDLER_MAPPING_BEAN}: if a future Spring version renamed the
     * canonical MVC handler-mapping bean, the {@code @Qualifier} would silently resolve to null. This fails at
     * build time when the Spring dependency is upgraded, so the rename cannot slip through unnoticed.
     */
    @Test
    void canonicalMvcHandlerMappingBeanNameStillResolves() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasBean(AuthzAutoConfiguration.MVC_HANDLER_MAPPING_BEAN);
                    assertThat(context.getBean(AuthzAutoConfiguration.MVC_HANDLER_MAPPING_BEAN))
                            .isInstanceOf(RequestMappingHandlerMapping.class);
                });
    }
}
