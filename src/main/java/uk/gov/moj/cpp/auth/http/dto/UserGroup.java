package uk.gov.moj.cpp.auth.http.dto;

public record UserGroup(
        String groupId,
        String groupName,
        String prosecutingAuthority
) {
}