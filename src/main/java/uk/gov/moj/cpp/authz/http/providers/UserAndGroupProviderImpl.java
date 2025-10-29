package uk.gov.moj.cpp.authz.http.providers;

import lombok.extern.slf4j.Slf4j;
import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;

import java.util.Set;

@Slf4j
public record UserAndGroupProviderImpl(AuthzPrincipal principal) implements UserAndGroupProvider {

    @Override
    public boolean isMemberOfAnyOfTheSuppliedGroups(final Action action, final String... groups) {
        log.info("UserAndGroupProviderImpl checking member groups for action:{}", action.getName());
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
}
