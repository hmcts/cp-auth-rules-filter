package uk.gov.moj.cpp.authz.drools;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.authz.http.AuthzPrincipal;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.dto.UserPermission;
import uk.gov.moj.cpp.authz.http.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.authz.testsupport.TestConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class DroolsAuthzEngineTest {


    private static final String DROOLS_CLASSPATH_PATTERN = "classpath:/drool-test/**/*.drl";
    private final List<UserPermission> userPermissionList = List.of(
            new UserPermission(UUID.randomUUID().toString(), "Restrict Details", "View", "Desc"),
            new UserPermission(UUID.randomUUID().toString(), "sjp-financial-means", "Delete", "Desc"),
            new UserPermission(UUID.randomUUID().toString(), "Reorder", "View", "Desc")
    );

    @BeforeAll
    static void prepare() {
        System.setProperty("mvel2.disable.jit", "true");
        System.setProperty("drools.compiler", "ECLIPSE");
        System.setProperty("drools.dialect.default", "java");
    }

    @Test
    @Timeout(5)
    void allowsWhenRuleMatchesActionAndGroup() {
        final HttpAuthzProperties properties = new HttpAuthzProperties();
        properties.setDroolsClasspathPattern(DROOLS_CLASSPATH_PATTERN);
        properties.setReloadOnEachRequest(false);
        properties.setDenyWhenNoRules(true);
        final DroolsAuthzEngine engine = new DroolsAuthzEngine(properties);

        final AuthzPrincipal principal =
                new AuthzPrincipal("u1", "fn", "ln", "u1@example.test", Set.of(TestConstants.GROUP_LA), List.of());
        final UserAndGroupProvider provider = getUserAndGroupProvider(principal);

        final Action action = new Action(TestConstants.ACTION_HELLO, Map.of());
        assertTrue(engine.evaluate(provider, action), "Should have access");
    }

    @Test
    @Timeout(5)
    void deniesWhenNoRuleMatches() {
        final HttpAuthzProperties properties = new HttpAuthzProperties();
        properties.setDroolsClasspathPattern(DROOLS_CLASSPATH_PATTERN);
        properties.setReloadOnEachRequest(false);
        properties.setDenyWhenNoRules(true);
        final DroolsAuthzEngine engine = new DroolsAuthzEngine(properties);

        final UserAndGroupProvider provider = getUserAndGroupProvider(null); //= (action, groups) -> false;
        final Action action = new Action(TestConstants.ACTION_ECHO, Map.of());
        assertFalse(engine.evaluate(provider, action), "Access Denied");
    }

    @Test
    @Timeout(5)
    void allowsWhenVendorActionSjpDeleteFinancialMeansAndGroupIsLaAndPermissionsDelete() {
        final HttpAuthzProperties properties = new HttpAuthzProperties();
        properties.setDroolsClasspathPattern(DROOLS_CLASSPATH_PATTERN);
        properties.setReloadOnEachRequest(false);
        properties.setDenyWhenNoRules(true);
        final DroolsAuthzEngine engine = new DroolsAuthzEngine(properties);


        final AuthzPrincipal principal =
                new AuthzPrincipal("u2", "fn", "ln", "u2@example.test", Set.of(TestConstants.GROUP_LA), userPermissionList);
        final UserAndGroupProvider provider = getUserAndGroupProvider(principal);

        final Action action = new Action(TestConstants.ACTION_SJP_DELETE_FINANCIAL_MEANS, Map.of());
        assertTrue(engine.evaluate(provider, action), "Expected allow for sjp.delete-financial-means and LA group");
    }

    @Test
    @Timeout(5)
    void allowsWhenVendorActionHearingGetDraftResultAndGroupIsLa() {
        final HttpAuthzProperties properties = new HttpAuthzProperties();
        properties.setDroolsClasspathPattern(DROOLS_CLASSPATH_PATTERN);
        properties.setReloadOnEachRequest(false);
        properties.setDenyWhenNoRules(true);
        final DroolsAuthzEngine engine = new DroolsAuthzEngine(properties);

        final AuthzPrincipal principal =
                new AuthzPrincipal("u3", "fn", "ln", "u3@example.test", Set.of(TestConstants.GROUP_LA), List.of());
        final UserAndGroupProvider provider = getUserAndGroupProvider(principal);

        final Action action = new Action(TestConstants.ACTION_HEARING_GET_DRAFT_RESULT, Map.of());
        assertTrue(engine.evaluate(provider, action), "Expected allow for hearing.get-draft-result and LA group");
    }

    @Test
    @Timeout(5)
    void allowsWhenVendorActionHearingGetDraftResultAndPermissionIsView() {
        final HttpAuthzProperties properties = new HttpAuthzProperties();
        properties.setDroolsClasspathPattern(DROOLS_CLASSPATH_PATTERN);
        properties.setReloadOnEachRequest(false);
        properties.setDenyWhenNoRules(true);
        final DroolsAuthzEngine engine = new DroolsAuthzEngine(properties);

        final AuthzPrincipal principal =
                new AuthzPrincipal("u3", "fn", "ln", "u3@example.test", Set.of(), userPermissionList);
        final UserAndGroupProvider provider = getUserAndGroupProvider(principal);

        final Action action = new Action(TestConstants.ACTION_HEARING_GET_DRAFT_RESULT, Map.of());
        assertTrue(engine.evaluate(provider, action), "Expected allow for hearing.get-draft-result and LA group");
    }

    private static UserAndGroupProvider getUserAndGroupProvider(final AuthzPrincipal principal) {

        final ObjectMapper objectMapper = new ObjectMapper();

        return new UserAndGroupProvider() {
            @Override
            public boolean isMemberOfAnyOfTheSuppliedGroups(final Action action, final String... groups) {
                if (nonNull(principal)) {
                    return Arrays.stream(groups)
                            .anyMatch(g -> principal.groups().stream().anyMatch(s -> s.equalsIgnoreCase(g)));
                }
                return false;
            }

            @Override
            public boolean hasPermission(final Action action, final String... expectedPermissions) {
                if (nonNull(principal)) {
                    final List<UserPermission> expectedUserPermissions = Arrays.stream(expectedPermissions).map(this::toUserPermission).toList();
                    return expectedUserPermissions.stream()
                            .map(UserPermission::getKey)
                            .anyMatch(eup -> principal.permissions().stream().map(UserPermission::getKey).anyMatch(pp -> pp.contains(eup)));
                }
                return false;
            }

            private UserPermission toUserPermission(final String permissionStr) {
                try {
                    return objectMapper.readValue(permissionStr, UserPermission.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
