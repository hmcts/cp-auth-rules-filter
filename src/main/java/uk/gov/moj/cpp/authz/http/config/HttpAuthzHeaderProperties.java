package uk.gov.moj.cpp.authz.http.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@AllArgsConstructor
public class HttpAuthzHeaderProperties {
    @Value("${auth.rules.actionRequired:true}")
    private boolean actionRequired;

    @Value("${auth.rules.userIdHeaderName:CJSCPPUID}")
    private String userIdHeaderName;

    @Value("${auth.rules.actionHeaderName:CPP-ACTION}")
    private String actionHeaderName;

    @Value("${auth.rules.acceptHeaderName}")
    private String acceptHeaderName;
}
