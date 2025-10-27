package uk.gov.moj.cpp.authz.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResolvedAction {
    private String name;
    private boolean vendorSupplied;
    private boolean headerSupplied;
}