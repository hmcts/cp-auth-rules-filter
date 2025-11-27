package uk.gov.moj.cpp.authz.http.dto;

import org.apache.commons.lang3.StringUtils;

public record UserPermission(
        String permissionId,
        String object,
        String action,
        String description
) {

    public String getKey() {
        StringBuilder stringBuilder = new StringBuilder();
        String strPermissionId = this.permissionId();
        String strObject = this.object();
        String strAction = this.action();
        String strDescription = this.description();

        if (StringUtils.isNotEmpty(strPermissionId)) {
            stringBuilder.append(strPermissionId).append("_");
        }

        if (StringUtils.isNotEmpty(strObject)) {
            stringBuilder.append(strObject).append("_");
        }

        if (StringUtils.isNotEmpty(strAction)) {
            stringBuilder.append(strAction).append("_");
        }

        if (StringUtils.isNotEmpty(strDescription)) {
            stringBuilder.append(strDescription);
        }
        return StringUtils.stripEnd(stringBuilder.toString(), "_");
    }
}