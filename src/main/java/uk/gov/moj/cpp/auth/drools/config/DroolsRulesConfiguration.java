package uk.gov.moj.cpp.auth.drools.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import uk.gov.moj.cpp.auth.drools.RuleAsset;
import uk.gov.moj.cpp.auth.drools.service.DroolsRulesFileService;

import java.io.IOException;
import java.util.List;

@Configuration
@AllArgsConstructor
@Slf4j
public class DroolsRulesConfiguration {
    @Bean
    public PathMatchingResourcePatternResolver resolver() {
        return new PathMatchingResourcePatternResolver();
    }

    @Bean
    public DroolsRulesFileService droolsRulesFileService() {
        return new DroolsRulesFileService(resolver());
    }

    @Bean
    public List<RuleAsset> loadDroolsRules() throws IOException {
        return droolsRulesFileService().getRules();
    }
}
