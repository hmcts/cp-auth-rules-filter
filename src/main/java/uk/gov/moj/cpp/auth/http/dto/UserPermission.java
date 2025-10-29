package uk.gov.moj.cpp.auth.http.dto;

public record UserPermission(
        String permissionId,
        String object,
        String action,
        String description
) {
}