package uk.gov.moj.cpp.auth.http;

import org.springframework.stereotype.Service;
import uk.gov.moj.cpp.auth.http.dto.UserGroup;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public final class DefaultIdentityToGroupsMapper implements IdentityToGroupsMapper {

    @Override
    public Set<String> toGroups(final IdentityResponse identityResponse) {
        final Set<String> groups = new LinkedHashSet<>();
        if (identityResponse != null && identityResponse.groups() != null) {
            for (final UserGroup userGroup : identityResponse.groups()) {
                if (userGroup == null) {
                    continue;
                }
                final String name = userGroup.groupName();
                if (name != null && !name.isBlank()) {
                    groups.add(name);
                }
                final String prosecutingAuthority = userGroup.prosecutingAuthority();
                if (prosecutingAuthority != null && !prosecutingAuthority.isBlank()) {
                    groups.add("Prosecuting Authority Access");
                }
            }
        }
        return groups;
    }
}
