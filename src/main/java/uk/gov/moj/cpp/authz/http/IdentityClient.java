package uk.gov.moj.cpp.authz.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cpp.authz.http.config.HttpAuthHeaderProperties;
import uk.gov.moj.cpp.authz.http.config.HttpAuthPathProperties;
import uk.gov.moj.cpp.authz.http.dto.LoggedInUserPermissionsResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public final class IdentityClient {
    private final HttpAuthPathProperties pathProperties;
    private final HttpAuthHeaderProperties headerProperties;
    private final RestTemplate restTemplate;

    public IdentityClient(final HttpAuthPathProperties pathProperties, final HttpAuthHeaderProperties headerProperties) {
        this.pathProperties = pathProperties;
        this.headerProperties = headerProperties;
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(21).toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * PMD LooseCoupling warning on HttpHeaders ... how can we fix this ?
     * MultiValueMap<String, String> httpHeaders = new HttpHeaders();
     * RequestEntity.get(url).headers((HttpHeaders) httpHeaders)
     * ... then get warned to use HttpHeaders to avoid the cast :)
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public IdentityResponse fetchIdentity(final UUID userId) {
        final String urlPath = pathProperties.getIdentityUrlPath().replace("{userId}", userId.toString());
        final URI url = constructUrl(pathProperties.getIdentityUrlRoot(), urlPath);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", headerProperties.getAcceptHeader());
        httpHeaders.add(headerProperties.getUserIdHeaderName(), userId.toString());
        final RequestEntity<Void> request = RequestEntity.get(url).headers(httpHeaders).build();
        try {
            final ResponseEntity<LoggedInUserPermissionsResponse> response = restTemplate.exchange(request, LoggedInUserPermissionsResponse.class);
            if (response == null || response.getBody() == null) {
                log.error("Empty identity response");
                return new IdentityResponse(userId, java.util.List.of(), java.util.List.of());
            } else {
                return new IdentityResponse(userId, response.getBody().groups(), response.getBody().permissions());
            }
        } catch (Exception e) {
            log.error("Exception fetching identity ", e);
            throw e;
        }
    }

    public URI constructUrl(final String root, final String path) {
        try {
            final URL rootUrl = new URL(root);
            final URI uri = new URL(rootUrl, path).toURI();
            if (uri.getHost().equalsIgnoreCase(rootUrl.getHost())) {
                return uri;
            } else {
                log.error("Invalid url constructed host does not match root host:{}", rootUrl.getHost());
                throw new RuntimeException("Invalid url host does not match urlRoot");
            }
        } catch (URISyntaxException | MalformedURLException e) {
            log.error("Invalid url. {}", e.getMessage());
            throw new RuntimeException("Invalid url");
        }
    }
}
