package uk.gov.moj.cpp.authz.http.providers;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;
import uk.gov.moj.cpp.authz.http.dto.UserPermission;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public record RequestUserAndGroupProvider(AuthzPrincipal principal,
                                          ObjectMapper objectMapper) implements UserAndGroupProvider {

    @Override
    public boolean isMemberOfAnyOfTheSuppliedGroups(final Action action, final String... groups) {
        boolean isMember = false;

        if (principal != null) {
            final Set<String> userGroups = principal.groups();

            if (userGroups != null && !userGroups.isEmpty() && groups != null) {
                for (final String requestedGroup : groups) {
                    if (requestedGroup != null) {
                        for (final String userGroup : userGroups) {
                            if (userGroup != null && userGroup.equalsIgnoreCase(requestedGroup)) {
                                isMember = true;
                                break;
                            }
                        }
                    }
                    if (isMember) {
                        break;
                    }
                }
            }
        }

        return isMember;
    }

    @Override
    public boolean hasPermission(final Action action, final String... expectedPermissions) {
        final List<UserPermission> expectedPermissionList = Arrays.stream(expectedPermissions)
                .map(this::getUserPermissionFromPermissionString)
                .collect(Collectors.toList());
        final List<UserPermission> permissions = nonNull(principal) ? principal.permissions() : emptyList();
        return this.isMemberInPermissionList(permissions, expectedPermissionList);
    }

    private boolean isMemberInPermissionList(final List<UserPermission> permissions, final List<UserPermission> expectedPermissions) {
        if (!expectedPermissions.isEmpty() && !permissions.isEmpty()) {
            HashSet<String> expectedPermissionSet = expectedPermissions.stream().map(UserPermission::getKey).collect(toCollection(HashSet::new));
            HashSet<String> actualPermissionSet = permissions.stream().map(UserPermission::getKey).collect(toCollection(HashSet::new));
            return expectedPermissionSet.stream().anyMatch(expectedPermissionKey ->
                    actualPermissionSet.stream().anyMatch(actualPermissionKey -> actualPermissionKey.contains(expectedPermissionKey))
            );
        }
        return false;
    }

    private UserPermission getUserPermissionFromPermissionString(final String userPermissionJson) {
        try {
            return this.objectMapper.readValue(userPermissionJson, UserPermission.class);
        } catch (IOException ioe) {
            throw new RuntimeException(String.format("Unable to convert Json String %s", userPermissionJson), ioe);
        }
    }

}
