package uk.gov.moj.cpp.authz.http.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "authz.http")
public class HttpAuthzProperties {

    @Builder.Default
    private boolean enabled = false;

    @Builder.Default
    private String identityUrlTemplate = "http://localhost:8080/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions?";

    @Builder.Default
    private String userIdHeader = "CJSCPPUID";

    @Builder.Default
    private String actionHeader = "CPP-ACTION";

    @Builder.Default
    private String acceptHeader = "application/vnd.usersgroups.get-logged-in-user-permissions+json";

    @Builder.Default
    private String droolsClasspathPattern = "classpath:/acl/**/*.drl";

    @Builder.Default
    private boolean reloadOnEachRequest = true;

    @Builder.Default
    private boolean actionRequired = false;

    @Builder.Default
    private boolean denyWhenNoRules = true;

    @Builder.Default
    private Map<String, String> groupAliases = new LinkedHashMap<>();

    @Builder.Default
    private Integer filterOrder = Ordered.HIGHEST_PRECEDENCE + 30;

    @Builder.Default
    private List<String> excludePathPrefixes = new ArrayList<>(List.of("/usersgroups-query-api/", "/actuator", "/error"));
}
