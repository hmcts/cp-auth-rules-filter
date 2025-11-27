package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.dto.UserPermission;

import java.util.List;
import java.util.Set;

public record AuthzPrincipal(
        String userId,
        String firstName,
        String lastName,
        String email,
        Set<String> groups,
        List<UserPermission> permissions
) {
}