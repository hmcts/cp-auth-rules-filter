package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PathExclusionCheckerTest {

    @Test
    void matchesSinglePrefix() {
        final PathExclusionChecker checker = new PathExclusionChecker(List.of("/usersgroups-query-api/"));

        assertTrue(checker.isExcluded("/usersgroups-query-api/query/api/rest/ping"));
    }

    @Test
    void doesNotMatchWhenNoPrefixMatches() {
        final PathExclusionChecker checker = new PathExclusionChecker(List.of("/usersgroups-query-api/", "/actuator/"));

        assertFalse(checker.isExcluded("/api/hello"));
    }

    @Test
    void matchesAnyOfMultiplePrefixes() {
        final PathExclusionChecker checker =
                new PathExclusionChecker(List.of("/health/", "/metrics/", "/usersgroups-query-api/"));

        assertTrue(checker.isExcluded("/metrics/prometheus"));
    }

    @Test
    void emptyPrefixListExcludesNothing() {
        final PathExclusionChecker checker = new PathExclusionChecker(List.of());

        assertFalse(checker.isExcluded("/anything"));
    }

    @Test
    void nullPrefixListIsTreatedAsEmpty() {
        final PathExclusionChecker checker = new PathExclusionChecker(null);

        assertFalse(checker.isExcluded("/anything"));
    }
}
