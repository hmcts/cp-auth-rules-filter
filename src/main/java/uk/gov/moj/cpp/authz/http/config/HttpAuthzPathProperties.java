package uk.gov.moj.cpp.authz.http.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
@AllArgsConstructor
public class HttpAuthzPathProperties {
    @Value("${auth.rules.identityUrlRoot}")
    private String identityUrlRoot;

    @Value("${auth.rules.identityUrlPath}")
    private String identityUrlPath;

    @Value("${auth.rules.excludePathPrefixes:\"/usersgroups-query-api/\", \"/actuator\", \"/error\"}")
    private List<String> excludePathPrefixes;
}
