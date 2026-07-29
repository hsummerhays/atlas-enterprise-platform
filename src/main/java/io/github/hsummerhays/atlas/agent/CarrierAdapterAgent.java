package io.github.hsummerhays.atlas.agent;

import io.github.hsummerhays.atlas.service.ClaudeService;
import io.github.hsummerhays.atlas.service.GitHubIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CarrierAdapterAgent implements AIAgent {

    private static final Logger log = LoggerFactory.getLogger(CarrierAdapterAgent.class);

    private static final Pattern CARRIER_LINE = Pattern.compile("^CARRIER:\\s*([A-Z0-9_]+)", Pattern.MULTILINE);
    private static final Pattern FILE_BLOCK = Pattern.compile(
            "===FILE:\\s*(.+?)\\s*===\\s*\\n(.*?)(?=\\n===FILE:|\\z)", Pattern.DOTALL);

    private static final String SYSTEM_PROMPT = """
            You are a senior Java/Spring Boot engineer extending a shipping middleware that integrates
            carriers through a CarrierAdapter pattern.

            Existing contract (do not change):
              package io.github.hsummerhays.atlas.adapter;
              public interface CarrierAdapter {
                  Carrier getCarrier();
                  Shipment bookShipment(Shipment shipment);
                  BigDecimal quoteRate(Shipment shipment);
                  ShipmentStatus trackShipment(String trackingNumber);
                  boolean cancelShipment(String trackingNumber);
              }
            Carrier is an enum at io.github.hsummerhays.atlas.domain.model.Carrier.
            Shipment exposes getSenderAddress()/getRecipientAddress() (Address records with contactName,
            companyName, street1, street2, city, stateOrProvince, postalCode, countryCode, phone) and
            getPackages() (List<PackageDetail> with weightInKg/lengthCm/widthCm/heightCm).

            Given the task below, identify the target carrier as a short UPPERCASE identifier (e.g. UPS, USPS)
            and generate exactly four files for it, following the existing adapter package convention
            io.github.hsummerhays.atlas.adapter.<carrier-lowercase>:
              1. <Carrier>Properties.java - @ConfigurationProperties holder (baseUrl, clientId, clientSecret, accountNumber)
              2. <Carrier>TokenService.java - OAuth2 client-credentials token caching service
              3. <Carrier>Adapter.java - implements CarrierAdapter, falls back to a mock booking when credentials are unset
              4. <Carrier>AdapterTest.java - JUnit 5 @SpringBootTest smoke test with @ActiveProfiles("test")

            Respond with ONLY the following format, no extra commentary or markdown fences:
            CARRIER: <UPPERCASE_CARRIER_NAME>
            ===FILE: <path/from/repo/root.java>===
            <full file content>
            ===FILE: <path/from/repo/root.java>===
            <full file content>
            (repeat for all four files, using real relative paths under src/main/java or src/test/java)
            """;

    private final GitHubIntegrationService githubService;
    private final ClaudeService claudeService;
    private final CarrierEnumRegistrar enumRegistrar;

    public CarrierAdapterAgent(GitHubIntegrationService githubService, ClaudeService claudeService) {
        this.githubService = githubService;
        this.claudeService = claudeService;
        this.enumRegistrar = new CarrierEnumRegistrar(githubService);
    }

    @Override
    public AgentResult run(AgentRequest request) {
        log.info("CarrierAdapterAgent generating carrier adapter scaffolding via Claude...");

        String taskDescription = request.inputData() != null && !request.inputData().isBlank()
                ? request.inputData()
                : "Scaffold a new USPS carrier adapter";

        String generated = claudeService.analyze(SYSTEM_PROMPT, taskDescription);

        Matcher carrierMatcher = CARRIER_LINE.matcher(generated);
        if (!carrierMatcher.find()) {
            return failure("Claude response did not include a CARRIER identifier");
        }
        String carrierName = carrierMatcher.group(1).toUpperCase();

        Map<String, String> files = new LinkedHashMap<>();
        Matcher fileMatcher = FILE_BLOCK.matcher(generated);
        while (fileMatcher.find()) {
            files.put(fileMatcher.group(1).trim(), fileMatcher.group(2).strip() + "\n");
        }
        if (files.isEmpty()) {
            return failure("Claude response did not include any generated files");
        }

        String branchName = "feature/agent-" + carrierName.toLowerCase() + "-adapter";
        String mainSha = githubService.getMainBranchSha();
        boolean branchCreated = githubService.createBranch(branchName, mainSha);
        if (!branchCreated) {
            return failure("Failed to create branch: " + branchName);
        }

        if (!enumRegistrar.ensureRegistered(branchName, carrierName)) {
            return failure("Failed to register " + carrierName + " in Carrier.java");
        }

        for (Map.Entry<String, String> file : files.entrySet()) {
            boolean pushed = githubService.pushFileToGitHub(
                    branchName, file.getKey(), file.getValue(), "AI: scaffold " + file.getKey());
            if (!pushed) {
                return failure("Failed to push generated file: " + file.getKey());
            }
        }

        String fileList = files.keySet().stream().map(p -> "- `" + p + "`").collect(Collectors.joining("\n"));
        String prBody = String.format("""
                ## AI-Generated %s Carrier Adapter
                This Pull Request was generated by **CarrierAdapterAgent** from the request:
                > %s

                ### Files generated:
                %s

                *AI Governance: HITL review required before merging.*
                """, carrierName, taskDescription, fileList);

        String prUrl = githubService.createPullRequest(
                "AI Integration: " + carrierName + " Carrier Adapter",
                branchName,
                "main",
                prBody,
                List.of(getRequiredApprovalLevel().name().toLowerCase())
        );

        String outputSummary = String.format(
                "Successfully scaffolded %s Carrier Adapter!\nPR URL: %s\nFiles generated:\n%s",
                carrierName, prUrl,
                files.keySet().stream().map(p -> "- " + p).collect(Collectors.joining("\n")));

        return new AgentResult("CarrierAdapterAgent", true, outputSummary, Map.of("prUrl", prUrl));
    }

    private AgentResult failure(String message) {
        log.warn("CarrierAdapterAgent failed: {}", message);
        return new AgentResult("CarrierAdapterAgent", false, message, Map.of());
    }

    @Override
    public ApprovalLevel getRequiredApprovalLevel() {
        return ApprovalLevel.ARCHITECTURE;
    }
}
