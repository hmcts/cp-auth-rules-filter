package uk.gov.moj.cpp.authz.drools;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

//         final String actionJson = "\"name\":\"GET /api/hello\",\"attributes\":{\"method\":\"GET\",\"path\":\"/api/hello\"}}";
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Action {
    private String name;
    private Map<String, Object> attributes;
}
