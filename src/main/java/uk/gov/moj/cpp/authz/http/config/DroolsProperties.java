package uk.gov.moj.cpp.authz.http.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Getter
public class DroolsProperties {
    @Value("${auth.rules.reloadOnEachRequest:true}")
    private boolean reloadOnEachRequest = true;

    @Value("${auth.rules.denyWhenNoRules:true}")
    private boolean denyWhenNoRules = true;

    @Value("${auth.rules.enabled:droolsClasspathPattern}")
    private String droolsClasspathPattern = "classpath:/acl/**/*.drl";
}
