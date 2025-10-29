package uk.gov.moj.cpp.auth.drools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AuthAction {
    private String name;
    private Map<String, Object> attributes;
}
