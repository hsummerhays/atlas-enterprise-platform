package io.github.hsummerhays.atlas.agent;

import io.github.hsummerhays.atlas.service.GitHubIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a new carrier constant to the Carrier enum source file via the GitHub contents API,
 * if it isn't already present. Isolated from CarrierAdapterAgent because splicing raw Java
 * source text is a distinct, fragile concern from prompting Claude and opening the PR.
 */
class CarrierEnumRegistrar {

    private static final Logger log = LoggerFactory.getLogger(CarrierEnumRegistrar.class);

    private static final String CARRIER_ENUM_PATH = "src/main/java/io/github/hsummerhays/atlas/domain/model/Carrier.java";

    private final GitHubIntegrationService githubService;

    CarrierEnumRegistrar(GitHubIntegrationService githubService) {
        this.githubService = githubService;
    }

    boolean ensureRegistered(String branchName, String carrierName) {
        try {
            GitHubIntegrationService.FileEntry carrierFile = githubService.getFileEntry(CARRIER_ENUM_PATH);
            if (carrierFile.content().contains(carrierName)) {
                return true;
            }
            String updatedContent = addCarrierConstant(carrierFile.content(), carrierName);
            return githubService.pushFileToGitHub(branchName, CARRIER_ENUM_PATH, updatedContent,
                    "AI: register " + carrierName + " enum value in Carrier.java", carrierFile.sha());
        } catch (Exception e) {
            log.error("Failed to read/update Carrier enum from GitHub", e);
            return false;
        }
    }

    private String addCarrierConstant(String content, String carrierName) {
        int closingBraceIdx = content.lastIndexOf('}');
        String before = content.substring(0, closingBraceIdx).stripTrailing();
        String addition = before.endsWith(",") ? "\n    " + carrierName : ",\n    " + carrierName;
        return before + addition + "\n" + content.substring(closingBraceIdx);
    }
}
