package uk.gov.moj.cpp.authz.http.dto;

import org.apache.commons.lang3.StringUtils;

public record UserPermission(
        String permissionId,
        String object,
        String action,
        String description
) {

    public String getKey() {
        final StringBuilder stringBuilder = new StringBuilder();
        final String strObject = this.object();
        final String strAction = this.action();

        if (StringUtils.isNotEmpty(strObject)) {
            stringBuilder.append(strObject).append('_');
        }

        if (StringUtils.isNotEmpty(strAction)) {
            stringBuilder.append(strAction).append('_');
        }

        return StringUtils.stripEnd(stringBuilder.toString(), "_");
    }
}