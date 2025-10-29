package uk.gov.moj.cpp.authz.drools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class RuleAsset {
    private String sourcePath;
    private String content;
}
