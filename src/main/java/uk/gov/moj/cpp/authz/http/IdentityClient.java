package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.dto.LoggedInUserPermissionsResponse;

import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal identity client that fetches group/permission information for a user.
 * Avoids concrete HttpHeaders usage by adding headers directly on the RequestEntity builder.
 */
public final class IdentityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);

    private final HttpAuthzProperties properties;
    private final RestTemplate restTemplate;

    public IdentityClient(final HttpAuthzProperties properties) {
        this.properties = properties;

        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(21).toMillis());

        this.restTemplate = new RestTemplate(factory);
    }

    public IdentityResponse fetchIdentity(final String userId) {
        final String template = properties.getIdentityUrlTemplate();
        final String url = template.contains("{userId}") ? template.replace("{userId}", userId) : template;

        final RequestEntity<Void> request = RequestEntity
                .get(URI.create(url))
                .header("Accept", properties.getAcceptHeader())
                .header(properties.getUserIdHeader(), userId)
                .build();

        try {
            final ResponseEntity<LoggedInUserPermissionsResponse> response =
                    restTemplate.exchange(request, LoggedInUserPermissionsResponse.class);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Identity fetch for userId={} returned status={}", userId, response.getStatusCode());
            }

            final LoggedInUserPermissionsResponse body = response.getBody();
            if (body == null) {
                LOGGER.warn("Empty identity response for userId={}", userId);
                return new IdentityResponse(userId, java.util.List.of(), java.util.List.of());
            }
            return new IdentityResponse(userId, body.groups(), body.permissions());

        } catch (final Exception ex) {
            LOGGER.error("Identity fetch failed for userId={} ({}). Returning empty identity.",
                    userId, ex.getClass().getSimpleName(), ex);
            return new IdentityResponse(userId, java.util.List.of(), java.util.List.of());
        }
    }
}
