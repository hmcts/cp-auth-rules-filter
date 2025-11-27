package uk.gov.moj.cpp.authz.http.providers;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;
import uk.gov.moj.cpp.authz.http.dto.UserPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RequestUserAndGroupProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsTrueWhenPrincipalHasAnyOfTheGroups() {
        final AuthzPrincipal principal = new AuthzPrincipal(
                "u1", "fn", "ln", "u1@example.test", Set.of("Legal Advisers", "Other"), List.of());
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(principal, objectMapper);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.isMemberOfAnyOfTheSuppliedGroups(action,
                "Prosecuting Authority Access", "Legal Advisers");

        Assertions.assertTrue(result, "Expected match when principal has one of the groups");
    }

    @Test
    void returnsFalseWhenPrincipalLacksGroups() {
        final AuthzPrincipal principal = new AuthzPrincipal(
                "u2", "fn", "ln", "u2@example.test", Set.of("Guests"), List.of());
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(principal, objectMapper);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.isMemberOfAnyOfTheSuppliedGroups(action,
                "Legal Advisers", "Prosecuting Authority Access");

        Assertions.assertFalse(result, "Expected no match when principal lacks groups");
    }

    @Test
    void returnsFalseWhenPrincipalIsNull() {
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(null, null);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.isMemberOfAnyOfTheSuppliedGroups(action, "Anything");

        Assertions.assertFalse(result, "Expected false when principal is null");
    }

    @Test
    void returnsTrueWhenPrincipalHasAnyOfThePermissions() {
        final AuthzPrincipal principal = new AuthzPrincipal(
                "u1", "fn", "ln", "u1@example.test", Set.of("Legal Advisers", "Other"), List.of(
                new UserPermission(randomUUID().toString(), "Restrict Details", "View", "Desc"),
                new UserPermission(randomUUID().toString(), "Reorder", "View", "Desc"),
                new UserPermission(randomUUID().toString(), "COTR", "courts-access", "Desc"),
                new UserPermission(randomUUID().toString(), "CrackedIneffective", "Edit", "Desc")
        ));
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(principal, objectMapper);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.hasPermission(action, "{\"object\":\"COTR\",\"action\":\"courts-access\"}", "{\"object\":\"random\",\"action\":\"run\"}");

        Assertions.assertTrue(result, "Expected match when principal has one of the permissions");
    }

    @Test
    void returnsFalseWhenPrincipalLacksPermissions() {
        final AuthzPrincipal principal = new AuthzPrincipal(
                "u1", "fn", "ln", "u1@example.test", Set.of("Legal Advisers", "Other"), List.of());
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(principal, objectMapper);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.hasPermission(action, "{\"object\":\"COTR\",\"action\":\"courts-access\"}", "{\"object\":\"random\",\"action\":\"run\"}");

        Assertions.assertFalse(result, "Expected no match when principal lacks permissions");
    }

    @Test
    void returnsFalseWhenPrincipalHasNoMatchingPermissions() {
        final AuthzPrincipal principal = new AuthzPrincipal(
                "u1", "fn", "ln", "u1@example.test", Set.of("Legal Advisers", "Other"), List.of(
                new UserPermission(randomUUID().toString(), "Restrict Details", "View", "Desc"),
                new UserPermission(randomUUID().toString(), "Reorder", "View", "Desc"),
                new UserPermission(randomUUID().toString(), "COTR", "courts-access", "Desc"),
                new UserPermission(randomUUID().toString(), "CrackedIneffective", "Edit", "Desc")
        ));
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(principal, objectMapper);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.hasPermission(action, "{\"object\":\"random1\",\"action\":\"view\"}", "{\"object\":\"random2\",\"action\":\"run\"}");

        Assertions.assertFalse(result, "Expected no match when principal permissions do not match with requested");
    }

    @Test
    void hasPermissionReturnsFalseWhenPrincipalIsNull() {
        final RequestUserAndGroupProvider provider = new RequestUserAndGroupProvider(null, objectMapper);
        final Action action = new Action("GET /api/hello", Map.of());

        final boolean result = provider.hasPermission(action, "{\"object\":\"random1\",\"action\":\"view\"}");

        Assertions.assertFalse(result, "Expected false when principal is null");
    }
}
