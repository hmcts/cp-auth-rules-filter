package uk.gov.moj.cpp.authz.drools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.authz.http.providers.UserAndGroupProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public final class DroolsAuthEngine {

    private List<RuleAsset> ruleAssets;

    public boolean evaluate(final UserAndGroupProvider userAndGroupProvider, final Action action) {
        try {
            if (ruleAssets == null || ruleAssets.isEmpty()) {
                throw new RuntimeException("No drools rules loaded");
            }
            final KieHelper kieHelper = new KieHelper();
            for (final RuleAsset asset : ruleAssets) {
                kieHelper.addResource(toResource(asset), ResourceType.DRL);
            }
            log.info("Drools has {} ruleAssets", ruleAssets.size());
            final Results verification = kieHelper.verify();
            if (verification.hasMessages(Message.Level.ERROR)) {
                log.error("Drools verification errors: {}", verification.getMessages(Message.Level.ERROR));
                return false;
            } else {
                try (KieSession kieSession = kieHelper.build().newKieSession()) {
                    final Outcome outcome = new Outcome();
                    kieSession.setGlobal("userAndGroupProvider", userAndGroupProvider);
                    kieSession.insert(outcome);
                    kieSession.insert(action);
                    kieSession.fireAllRules();
                    log.info("Drools outcome {}", outcome.isSuccess());
                    return outcome.isSuccess();
                }
            }
        } catch (final Exception exception) {
            log.error("Drools evaluation failed with unexpected exception", exception);
            return false;
        }
    }

    private static Resource toResource(final RuleAsset asset) {
        final Resource resource = ResourceFactory.newByteArrayResource(asset.getContent().getBytes(StandardCharsets.UTF_8));
        resource.setSourcePath(asset.getSourcePath());
        return resource;
    }
}
