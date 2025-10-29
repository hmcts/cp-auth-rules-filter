package uk.gov.moj.cpp.authz.drools.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import uk.gov.moj.cpp.authz.drools.RuleAsset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DroolsRulesFileService {
    public static final String DROOLS_RESOURCE_FOLDER = "classpath:/drools/**/*.drl";
    public static final String DROOLS_FOLDER_PREFIX = "uk";

    private PathMatchingResourcePatternResolver patternResolver;

    @SneakyThrows
    public List<RuleAsset> getRules() {
        log.info("Loading drools rules matching pattern:{}", DROOLS_RESOURCE_FOLDER);
        final Resource[] resources = patternResolver.getResources(DROOLS_RESOURCE_FOLDER);
        final List<RuleAsset> ruleAssetList = new ArrayList<>();
        for (final Resource resource : resources) {
            final String content = readResourceContent(resource);
            final String sourcePath = getRulesFilePackageFolder(resource.getURI().getPath());
            log.info("Added drools rules file:{} from {}", sourcePath, resource.getURI().getPath());
            ruleAssetList.add(new RuleAsset(sourcePath, content));
        }
        ruleAssetList.sort(Comparator.comparing(asset -> asset.getSourcePath()));
        return ruleAssetList;
    }

    public String getRulesFilePackageFolder(String filepath) {
        return filepath.replaceAll(".*/" + DROOLS_FOLDER_PREFIX, "uk");
    }

    private String readResourceContent(final Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return sanitize(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String sanitize(final String input) {
        String sanitized = input == null ? "" : input;
        if (!sanitized.isEmpty() && sanitized.charAt(0) == '\uFEFF') {
            sanitized = sanitized.substring(1);
        }
        sanitized = sanitized.replace("\u200B", "").replace("\u200C", "").replace("\u200D", "");
        return sanitized;
    }
}
