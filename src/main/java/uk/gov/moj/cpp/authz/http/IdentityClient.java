package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.dto.LoggedInUserPermissionsResponse;

import java.net.URI;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal identity client that fetches group/permission information for a user.
 * Avoids concrete HttpHeaders usage by adding headers directly on the RequestEntity builder.
 */
@Slf4j
public final class IdentityClient {

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
        final String sanitizedUserId = Encode.forJava(userId);
        final String template = properties.getIdentityUrlTemplate();
        final String url = template.contains("{userId}") ? template.replace("{userId}", sanitizedUserId) : template;
        final RequestEntity<Void> request = RequestEntity
                .get(URI.create(url))
                .header("Accept", properties.getAcceptHeader())
                .header(properties.getUserIdHeader(), sanitizedUserId)
                .build();

        try {
            final ResponseEntity<LoggedInUserPermissionsResponse> response = restTemplate.exchange(request, LoggedInUserPermissionsResponse.class);
            log.info("Identity fetch for userId={} returned status={}", sanitizedUserId, response.getStatusCode());
            final LoggedInUserPermissionsResponse body = response.getBody();
            if (body == null) {
                log.warn("Empty identity response for userId={}", sanitizedUserId);
                return new IdentityResponse(sanitizedUserId, java.util.List.of(), java.util.List.of());
            }
            return new IdentityResponse(sanitizedUserId, body.groups(), body.permissions());
        } catch (final Exception ex) {
            log.error("Identity fetch failed for userId={} ({}). Returning empty identity.", sanitizedUserId, ex.getClass().getSimpleName(), ex);
            return new IdentityResponse(sanitizedUserId, java.util.List.of(), java.util.List.of());
        }
    }
}
