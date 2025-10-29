package uk.gov.moj.cpp.authz.drools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import uk.gov.moj.cpp.authz.drools.service.DroolsRulesFileService;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;
import uk.gov.moj.cpp.authz.http.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.authz.testsupport.TestConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DroolsAuthEngineTest {

    private static final UUID USER_ID = UUID.randomUUID();

    List<RuleAsset> rules = new DroolsRulesFileService(new PathMatchingResourcePatternResolver()).getRules();

    @Test
    @Timeout(5)
    void allowsWhenRuleMatchesActionAndGroup() {
        final DroolsAuthEngine engine = new DroolsAuthEngine(rules);

        final AuthzPrincipal principal =
                new AuthzPrincipal(USER_ID, "fn", "ln", "u1@example.test", Set.of(TestConstants.GROUP_LA));
        final UserAndGroupProvider provider = (action, groups) -> {
            for (final String g : groups) {
                if (principal.groups().stream().anyMatch(s -> s.equalsIgnoreCase(g))) {
                    return true;
                }
            }
            return false;
        };
        final Action action = new Action(TestConstants.ACTION_HELLO, Map.of());
        assertTrue(engine.evaluate(provider, action), "Should have access");
    }

    @Test
    @Timeout(5)
    void deniesWhenNoRuleMatches() {
        final DroolsAuthEngine engine = new DroolsAuthEngine(rules);

        final UserAndGroupProvider provider = (action, groups) -> false;
        final Action action = new Action(TestConstants.ACTION_ECHO, Map.of());
        assertFalse(engine.evaluate(provider, action), "Access Denied");
    }

    @Test
    @Timeout(5)
    void allowsWhenVendorActionSjpDeleteFinancialMeansAndGroupIsLa() {
        final DroolsAuthEngine engine = new DroolsAuthEngine(rules);
        final AuthzPrincipal principal =
                new AuthzPrincipal(USER_ID, "fn", "ln", "u2@example.test", Set.of(TestConstants.GROUP_LA));
        final UserAndGroupProvider provider = (action, groups) -> {
            for (final String g : groups) {
                if (principal.groups().stream().anyMatch(s -> s.equalsIgnoreCase(g))) {
                    return true;
                }
            }
            return false;
        };

        final Action action = new Action(TestConstants.ACTION_SJP_DELETE_FINANCIAL_MEANS, Map.of());
        assertTrue(engine.evaluate(provider, action), "Expected allow for sjp.delete-financial-means and LA group");
    }

    @Test
    @Timeout(5)
    void allowsWhenVendorActionHearingGetDraftResultAndGroupIsLa() {
        final DroolsAuthEngine engine = new DroolsAuthEngine(rules);
        final AuthzPrincipal principal =
                new AuthzPrincipal(USER_ID, "fn", "ln", "u3@example.test", Set.of(TestConstants.GROUP_LA));
        final UserAndGroupProvider provider = (action, groups) -> {
            for (final String g : groups) {
                if (principal.groups().stream().anyMatch(s -> s.equalsIgnoreCase(g))) {
                    return true;
                }
            }
            return false;
        };

        final Action action = new Action(TestConstants.ACTION_HEARING_GET_DRAFT_RESULT, Map.of());
        assertTrue(engine.evaluate(provider, action), "Expected allow for hearing.get-draft-result and LA group");
    }
}
