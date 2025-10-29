package uk.gov.moj.cpp.auth.drools.providers;

import uk.gov.moj.cpp.auth.drools.AuthAction;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface UserAndGroupProvider {
    boolean isMemberOfAnyOfTheSuppliedGroups(AuthAction authAction, String... groups);
}
