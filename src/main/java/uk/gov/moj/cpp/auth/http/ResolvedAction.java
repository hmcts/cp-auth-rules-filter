package uk.gov.moj.cpp.auth.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResolvedAction {
    private String actionName;
    private boolean vendorSupplied;
    private boolean headerSupplied;
}