package uk.gov.moj.cpp.authz.http.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class DroolsProperties {
    private final boolean reloadOnEachRequest;
    private final boolean denyWhenNoRules;
    private final String droolsClasspathPattern;

    public DroolsProperties(@Value("${auth.drools.reloadOnEachRequest}") final boolean reloadOnEachRequest,
                            @Value("${auth.drools.denyWhenNoRules}") final boolean denyWhenNoRules,
                            @Value("${auth.drools.droolsClasspathPattern}") final String droolsClasspathPattern) {
        this.reloadOnEachRequest = reloadOnEachRequest;
        this.denyWhenNoRules = denyWhenNoRules;
        this.droolsClasspathPattern = droolsClasspathPattern;
    }
}
