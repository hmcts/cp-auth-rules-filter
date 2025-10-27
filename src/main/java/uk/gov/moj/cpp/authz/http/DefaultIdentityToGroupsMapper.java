package uk.gov.moj.cpp.authz.http;

import org.springframework.stereotype.Service;
import uk.gov.moj.cpp.authz.http.dto.UserGroup;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public final class DefaultIdentityToGroupsMapper implements IdentityToGroupsMapper {

    @Override
    public Set<String> toGroups(final IdentityResponse identityResponse) {
        final Map<String, String> aliasesAlwaysEmpty = new HashMap<>();
        final Set<String> groups = new LinkedHashSet<>();
        if (identityResponse != null && identityResponse.groups() != null) {
            for (final UserGroup userGroup : identityResponse.groups()) {
                if (userGroup == null) {
                    continue;
                }
                final String name = userGroup.groupName();
                if (name != null && !name.isBlank()) {
                    final String canonical = aliasesAlwaysEmpty.getOrDefault(name, name);
                    groups.add(canonical);
                }
                final String prosecutingAuthority = userGroup.prosecutingAuthority();
                if (prosecutingAuthority != null && !prosecutingAuthority.isBlank()) {
                    final String paCanonical = aliasesAlwaysEmpty.getOrDefault("Prosecuting Authority Access", "Prosecuting Authority Access");
                    groups.add(paCanonical);
                }

            }
        }
        return groups;
    }
}
