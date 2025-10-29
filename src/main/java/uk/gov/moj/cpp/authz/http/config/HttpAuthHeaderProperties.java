package uk.gov.moj.cpp.authz.http.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class HttpAuthHeaderProperties {
    private final boolean actionRequired;
    private final String userIdHeaderName;
    private final String actionHeaderName;
    private final String acceptHeader;

    public HttpAuthHeaderProperties(
            @Value("${auth.rules.actionRequired:true}") final boolean actionRequired,
            @Value("${auth.rules.userIdHeaderName:CJSCPPUID}") final String userIdHeaderName,
            @Value("${auth.rules.actionHeaderName:CPP-ACTION}") final String actionHeaderName,
            @Value("${auth.rules.acceptHeader:application/vnd.usersgroups.get-logged-in-user-permissions+json}") final String acceptHeader) {
        this.actionRequired = actionRequired;
        this.userIdHeaderName = userIdHeaderName;
        this.actionHeaderName = actionHeaderName;
        this.acceptHeader = acceptHeader;
    }
}