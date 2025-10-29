package uk.gov.hmcts.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cpp.authz.http.dto.LoggedInUserPermissionsResponse;
import uk.gov.moj.cpp.authz.http.dto.UserGroup;

import java.util.List;

@RestController
public class IdentityControllerForDemo {
    private static final String USER_LA_1 = "b066839e-30bd-42d9-8101-38cf039d673f";
    private static final String ACCESS_ALL = "ALL";
    private static final String DA_USER_1 = "d6eab103-fceb-4dd7-bc31-ef096dc12dee";

    @GetMapping(
            value = "/usersgroups-query-api/query/api/rest/usersgroups/users/{userId}/permissions",
            produces = "application/vnd.usersgroups.get-logged-in-user-permissions+json")
    public ResponseEntity<LoggedInUserPermissionsResponse> getPermissions(
            @PathVariable("userId") final String userId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.usersgroups.get-logged-in-user-permissions+json"))
                .body(sampleFor(userId));
    }

    private LoggedInUserPermissionsResponse sampleFor(final String userId) {
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
