package uk.gov.moj.cpp.authz.drools;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.authz.http.config.DroolsProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public final class DroolsAuthzEngine {

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*$", Pattern.MULTILINE);

    private final DroolsProperties properties;
    private volatile List<RuleAsset> ruleAssets;

    public DroolsAuthzEngine(final DroolsProperties properties) {
        this.properties = properties;
    }

    private static String sanitize(final String input) {
        String sanitized = input == null ? "" : input;
        if (!sanitized.isEmpty() && sanitized.charAt(0) == '\uFEFF') {
            sanitized = sanitized.substring(1);
        }
        sanitized = sanitized.replace("\u200B", "").replace("\u200C", "").replace("\u200D", "");
        return sanitized;
    }

    private static String resolveSourcePath(final String content, final String fallbackFileName) {
        String packagePath = "";
        final Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            packagePath = matcher.group(1).replace('.', '/') + "/";
        }
        final String fileName =
                (fallbackFileName != null && !fallbackFileName.isBlank()) ? fallbackFileName : "rules.drl";
        return packagePath + fileName;
    }

    private static org.kie.api.io.Resource toResource(final RuleAsset asset) {
        final org.kie.api.io.Resource resource =
                ResourceFactory.newByteArrayResource(asset.content.getBytes(StandardCharsets.UTF_8));
        resource.setSourcePath(asset.sourcePath);
        return resource;
    }

    private String readResourceContent(final Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return sanitize(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private synchronized void loadRules() throws IOException {
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final Resource[] resources = resolver.getResources(properties.getDroolsClasspathPattern());
        final List<RuleAsset> loaded = new ArrayList<>();
        int sequence = 0;
        for (final Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "file" + (sequence++) + ".drl";
            }
            final String content = readResourceContent(resource);
            final String sourcePath = resolveSourcePath(content, fileName);
            loaded.add(new RuleAsset(content, sourcePath));
        }
        loaded.sort(Comparator.comparing(asset -> asset.sourcePath));
        this.ruleAssets = loaded;
        if (log.isInfoEnabled()) {
            final List<String> paths = loaded.stream().map(asset -> asset.sourcePath).toList();
            log.info("Loaded {} DRL resource(s): {}", loaded.size(), paths);
        }
    }

    private void ensureRules() throws IOException {
        if (ruleAssets == null || properties.isReloadOnEachRequest()) {
            loadRules();
        }
    }

    public boolean evaluate(final Object userAndGroupProvider, final Action action) {
        boolean result;
        try {
            ensureRules();
            if (ruleAssets == null || ruleAssets.isEmpty()) {
                result = !properties.isDenyWhenNoRules();
            } else {
                final KieHelper kieHelper = new KieHelper();
                for (final RuleAsset asset : ruleAssets) {
                    kieHelper.addResource(toResource(asset), ResourceType.DRL);
                }
                final Results verification = kieHelper.verify();
                if (verification.hasMessages(Message.Level.ERROR)) {
                    if (log.isErrorEnabled()) {
                        log.error("Drools verification errors: {}", verification.getMessages(Message.Level.ERROR));
                    }
                    result = !properties.isDenyWhenNoRules();
                } else {
                    try (KieSession kieSession = kieHelper.build().newKieSession()) {
                        final Outcome outcome = new Outcome();
                        kieSession.setGlobal("userAndGroupProvider", userAndGroupProvider);
                        kieSession.insert(outcome);
                        kieSession.insert(action);
                        kieSession.fireAllRules();
                        result = outcome.isSuccess();
                    }
                }
            }
        } catch (final Exception exception) {
            if (log.isErrorEnabled()) {
                log.error("Drools evaluation failed; denying access", exception);
            }
            result = false;
        }
        return result;
    }

    private record RuleAsset(String content, String sourcePath) {
    }
}
