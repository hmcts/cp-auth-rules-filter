package uk.gov.moj.cpp.auth.http;

import uk.gov.moj.cpp.auth.http.dto.UserGroup;
import uk.gov.moj.cpp.auth.http.dto.UserPermission;

import java.util.List;
import java.util.UUID;

public record IdentityResponse(
        UUID userId,
        List<UserGroup> groups,
        List<UserPermission> permissions
) {
}