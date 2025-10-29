package uk.gov.moj.cpp.authz.http.providers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.authz.drools.AuthAction;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

class UserAndGroupProviderTest {

    UUID userId = UUID.randomUUID();

    @Test
    void returnsTrueWhenPrincipalHasAnyOfTheGroups() {
        final AuthzPrincipal principal = new AuthzPrincipal(userId, "fn", "ln", "u1@example.test", Set.of("Legal Advisers", "Other"));
        final UserAndGroupProviderImpl provider = new UserAndGroupProviderImpl(principal);
        final AuthAction authAction = new AuthAction("GET /api/hello", Map.of());

        final boolean result = provider.isMemberOfAnyOfTheSuppliedGroups(authAction,
                "Prosecuting Authority Access", "Legal Advisers");

        Assertions.assertTrue(result, "Expected match when principal has one of the groups");
    }

    @Test
    void returnsFalseWhenPrincipalLacksGroups() {
        final AuthzPrincipal principal = new AuthzPrincipal(
                userId, "fn", "ln", "u2@example.test", Set.of("Guests"));
        final UserAndGroupProviderImpl provider = new UserAndGroupProviderImpl(principal);
        final AuthAction authAction = new AuthAction("GET /api/hello", Map.of());

        final boolean result = provider.isMemberOfAnyOfTheSuppliedGroups(authAction,
                "Legal Advisers", "Prosecuting Authority Access");

        Assertions.assertFalse(result, "Expected no match when principal lacks groups");
    }

    @Test
    void returnsFalseWhenPrincipalIsNull() {
        final UserAndGroupProviderImpl provider = new UserAndGroupProviderImpl(null);
        final AuthAction authAction = new AuthAction("GET /api/hello", Map.of());

        final boolean result = provider.isMemberOfAnyOfTheSuppliedGroups(authAction, "Anything");

        Assertions.assertFalse(result, "Expected false when principal is null");
    }
}
