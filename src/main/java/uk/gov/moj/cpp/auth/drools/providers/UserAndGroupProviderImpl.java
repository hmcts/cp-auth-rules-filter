package uk.gov.moj.cpp.auth.drools.providers;

import lombok.extern.slf4j.Slf4j;
import uk.gov.moj.cpp.auth.drools.AuthAction;
import uk.gov.moj.cpp.auth.http.AuthPrincipal;

import java.util.Set;

@Slf4j
public record UserAndGroupProviderImpl(AuthPrincipal principal) implements UserAndGroupProvider {

    @Override
    public boolean isMemberOfAnyOfTheSuppliedGroups(final AuthAction authAction, final String... groups) {
        log.info("UserAndGroupProviderImpl checking member groups for action:{}", authAction.getName());
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
