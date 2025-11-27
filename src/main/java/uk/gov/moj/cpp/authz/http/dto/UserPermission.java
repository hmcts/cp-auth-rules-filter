package uk.gov.moj.cpp.authz.http.dto;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.stripEnd;

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

        if (isNotEmpty(strObject)) {
            stringBuilder.append(strObject).append('_');
        }

        if (isNotEmpty(strAction)) {
            stringBuilder.append(strAction).append('_');
        }

        return stripEnd(stringBuilder.toString(), "_");
    }
}