package uk.gov.moj.cpp.authz.http.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class HttpAuthzHeaderProperties {

    public HttpAuthzHeaderProperties(
            @Value("${auth.rules.actionRequired:true}") boolean actionRequired,
            @Value("${auth.rules.userIdHeaderName:CJSCPPUID}") String userIdHeaderName,
            @Value("${auth.rules.actionHeaderName:CPP-ACTION}") String actionHeaderName,
            @Value("${auth.rules.acceptHeader:application/vnd.usersgroups.get-logged-in-user-permissions+json}") String acceptHeader) {
        this.actionRequired = actionRequired;
        this.userIdHeaderName = userIdHeaderName;
        this.actionHeaderName = actionHeaderName;
        this.acceptHeader = acceptHeader;
    }

    private boolean actionRequired;
    private String userIdHeaderName;
    private String actionHeaderName;
    private String acceptHeader;
}