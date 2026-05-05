package uk.gov.moj.cpp.authz.http;

import java.util.List;

public final class PathExclusionChecker {

    private final List<String> prefixes;

    public PathExclusionChecker(final List<String> prefixes) {
        this.prefixes = prefixes == null ? List.of() : List.copyOf(prefixes);
    }

    public boolean isExcluded(final String pathWithinApplication) {
        for (final String prefix : prefixes) {
            if (pathWithinApplication.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
