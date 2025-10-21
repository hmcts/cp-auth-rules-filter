package uk.gov.moj.cpp.authz.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.dto.LoggedInUserPermissionsResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;

@Slf4j
public final class IdentityClient {
    private final static String USERID_REGEX = "^[\\-a-zA-Z0-9]*$";
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
        sanitizeUserId(userId);
        final String identityUrl = properties.getIdentityUrlTemplate();
        final String url = identityUrl.contains("{userId}") ? identityUrl.replace("{userId}", userId) : identityUrl;
        sanitizeUrl(url);
        final HttpHeaders headers = new HttpHeaders();
        final IdentityResponse identityResponse;
        headers.add("Accept", properties.getAcceptHeader());
        headers.add(properties.getUserIdHeader(), userId);

        final RequestEntity<Void> request = RequestEntity.get(URI.create(url)).headers(headers).build();
        final ResponseEntity<LoggedInUserPermissionsResponse> response = restTemplate.exchange(request, LoggedInUserPermissionsResponse.class);
        final LoggedInUserPermissionsResponse body = response.getBody();
        if (body == null) {
            log.warn("Empty identity response for userId={}", userId);
            identityResponse = new IdentityResponse(userId, java.util.List.of(), java.util.List.of());
        } else {
            identityResponse = new IdentityResponse(userId, body.groups(), body.permissions());
        }
        return identityResponse;
    }

    public void sanitizeUrl(String url) {
        try {
            new URL(url).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            log.error("Invalid url:{}", url);
            throw new RuntimeException("Invalid url");
        }
    }

    public void sanitizeUserId(String userId) {
        if (userId == null || userId.matches(USERID_REGEX)) {
            return;
        }
        log.error("Illegal userId \"{}\" must match regex:{}", userId, USERID_REGEX);
        throw new RuntimeException("Illegal userId");
    }
}
