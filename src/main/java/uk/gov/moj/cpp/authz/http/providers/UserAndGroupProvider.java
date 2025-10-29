package uk.gov.moj.cpp.authz.http.providers;

import uk.gov.moj.cpp.authz.drools.AuthAction;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface UserAndGroupProvider {
    boolean isMemberOfAnyOfTheSuppliedGroups(AuthAction authAction, String... groups);
}
