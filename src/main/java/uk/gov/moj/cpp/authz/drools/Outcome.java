package uk.gov.moj.cpp.authz.drools;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter // sadly we need a mutable setter as setSuccess called in the drools rules
public final class Outcome {
    private boolean success;

    @Override
    public String toString() {
        return "Outcome{success=" + success + '}';
    }
}
