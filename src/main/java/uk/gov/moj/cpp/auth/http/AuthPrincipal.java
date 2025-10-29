package uk.gov.moj.cpp.auth.http;

import java.util.Set;
import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Set<String> groups
) {
}