package uk.gov.moj.cpp.authz.integration.testclasses;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static uk.gov.moj.cpp.authz.testsupport.TestConstants.DA_USER_1;
import static uk.gov.moj.cpp.authz.testsupport.TestConstants.USER_LA_1;

@RestController
@AllArgsConstructor
@Slf4j
public class DummyIdentifyController {

    private static final String ACCESS_ALL = "ALL";

    public record UserGroup(String groupId, String groupName, String prosecutingAuthority) {
    }

    public record SwitchableRole(String roleId, String roleName) {
    }

    public record UserPermission(String permissionId, String object, String action, String description) {
    }

    public record LoggedInUserPermissionsResponse(
            List<UserGroup> groups, List<SwitchableRole> switchableRoles, List<UserPermission> permissions) {
    }

    @GetMapping(
            value = "/usersgroups-query-api/{userId}/permissions",
            produces = "application/vnd.usersgroups.get-logged-in-user-permissions+json")
    public ResponseEntity<LoggedInUserPermissionsResponse> getPermissions(
            @PathVariable("userId") final String userId) {
        log.info("DummyIdentityController received permissions request for userId:{}", userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.usersgroups.get-logged-in-user-permissions+json"))
                .body(responseForUser(userId));
    }

    private LoggedInUserPermissionsResponse responseForUser(final String userId) {
        final LoggedInUserPermissionsResponse response;
        if (USER_LA_1.equalsIgnoreCase(userId)) {
            final List<UserGroup> groups = List.of(
                    new UserGroup("63cae459-0e51-4d60-bcf8-c5324be50ba4", "Legal Advisers", ACCESS_ALL),
                    new UserGroup("53292fc8-d164-4a6c-8722-cdbc795cf83a", "Court Administrators", ACCESS_ALL)
            );
            response = new LoggedInUserPermissionsResponse(groups, List.of(), List.of());
        } else if (DA_USER_1.equalsIgnoreCase(userId)) {
            final List<UserGroup> groups = List.of(
                    new UserGroup("63cae459-0e51-4d60-bcf8-c5324be50ba4", "Defence Lawyer", ACCESS_ALL),
                    new UserGroup("53292fc8-d164-4a6c-8722-cdbc795cf83a", "Court Clerk", ACCESS_ALL)
            );
            response = new LoggedInUserPermissionsResponse(groups, List.of(), List.of());
        } else {
            final List<UserGroup> groups = List.of(new UserGroup("guest", "Guests", null));
            response = new LoggedInUserPermissionsResponse(groups, List.of(), List.of());
        }
        return response;
    }
}
