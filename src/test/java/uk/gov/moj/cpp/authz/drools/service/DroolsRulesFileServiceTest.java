package uk.gov.moj.cpp.authz.drools.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import uk.gov.moj.cpp.authz.drools.RuleAsset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.authz.drools.service.DroolsRulesFileService.DROOLS_RESOURCE_FOLDER;

@ExtendWith(MockitoExtension.class)
class DroolsRulesFileServiceTest {

    @Mock
    private PathMatchingResourcePatternResolver patternResolver;

    @InjectMocks
    private DroolsRulesFileService fileService;

    @Mock
    private Resource resource;
    @Mock
    private InputStream inputStream;

    @Test
    void local_rules_file_should_get_path_correctly() {
        String localFilename = "/projects/cp-springboot-auth-rules-filter/build/resources/test/drools-test/uk/gov/moj/cpp/authz/demo/test-rules.drl";
        assertThat(fileService.getRulesFilePackageFolder(localFilename)).isEqualTo("uk/gov/moj/cpp/authz/demo/test-rules.drl");
    }

    @Test
    void get_rules_should_load_drlfiles() throws IOException {
        when(patternResolver.getResources(DROOLS_RESOURCE_FOLDER)).thenReturn(new Resource[]{resource});
        when(resource.getURI()).thenReturn(URI.create("/home/project/uk/test.drl"));
        when(resource.getInputStream()).thenReturn(inputStream);
        when(inputStream.readAllBytes()).thenReturn("Rule".getBytes());
        List<RuleAsset> rules = fileService.getRules();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getSourcePath()).isEqualTo("uk/test.drl");
    }
}