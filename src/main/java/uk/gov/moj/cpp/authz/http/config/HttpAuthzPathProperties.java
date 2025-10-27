package uk.gov.moj.cpp.authz.http.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class HttpAuthzPathProperties {

    public HttpAuthzPathProperties(@Value("${auth.rules.identityUrlRoot}") String identityUrlRoot,
                                   @Value("${auth.rules.identityUrlPath}") String identityUrlPath,
                                   @Value("${auth.rules.excludePathPrefixes}") List<String> excludePathPrefixes) {
        this.identityUrlRoot = identityUrlRoot;
        this.identityUrlPath = identityUrlPath;
        this.excludePathPrefixes = excludePathPrefixes;
    }

    private String identityUrlRoot;
    private String identityUrlPath;
    private List<String> excludePathPrefixes;
}
