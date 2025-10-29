package uk.gov.moj.cpp.authz.http.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class HttpAuthPathProperties {
    private final Boolean disabled;
    private final String identityUrlRoot;
    private final String identityUrlPath;
    private final List<String> excludePathPrefixes;

    public HttpAuthPathProperties(@Value("${auth.rules.disabled:false}") final Boolean disabled,
                                  @Value("${auth.rules.identityUrlRoot}") final String identityUrlRoot,
                                  @Value("${auth.rules.identityUrlPath}") final String identityUrlPath,
                                  @Value("${auth.rules.excludePathPrefixes}") final List<String> excludePathPrefixes) {
        this.disabled = disabled;
        this.identityUrlRoot = identityUrlRoot;
        this.identityUrlPath = identityUrlPath;
        this.excludePathPrefixes = excludePathPrefixes;
    }
}
