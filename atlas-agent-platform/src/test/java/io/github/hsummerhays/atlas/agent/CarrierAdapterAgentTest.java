package io.github.hsummerhays.atlas.agent;

import io.github.hsummerhays.atlas.service.ClaudeService;
import io.github.hsummerhays.atlas.service.GitHubIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrierAdapterAgentTest {

    @Mock
    private GitHubIntegrationService githubService;

    @Mock
    private ClaudeService claudeService;

    private CarrierAdapterAgent agent;

    @BeforeEach
    void setUp() {
        agent = new CarrierAdapterAgent(githubService, claudeService);
    }

    @Test
    void run_wellFormedClaudeResponse_registersCarrierAndOpensPr() {
        String claudeResponse = """
                CARRIER: USPS
                ===FILE: src/main/java/io/github/hsummerhays/atlas/adapter/usps/UspsProperties.java===
                package io.github.hsummerhays.atlas.adapter.usps;
                public class UspsProperties {}
                ===FILE: src/main/java/io/github/hsummerhays/atlas/adapter/usps/UspsAdapter.java===
                package io.github.hsummerhays.atlas.adapter.usps;
                public class UspsAdapter {}
                """;
        when(claudeService.analyze(any(), any())).thenReturn(claudeResponse);
        when(githubService.getMainBranchSha()).thenReturn("main-sha");
        when(githubService.createBranch(any(), any())).thenReturn(true);
        when(githubService.getFileEntry(any())).thenReturn(new GitHubIntegrationService.FileEntry(
                "public enum Carrier {\n    FEDEX,\n    UPS,\n    DHL\n}\n", "carrier-sha"));
        when(githubService.pushFileToGitHub(any(), any(), any(), any(), any())).thenReturn(true);
        when(githubService.pushFileToGitHub(any(), any(), any(), any())).thenReturn(true);
        when(githubService.createPullRequest(any(), any(), any(), any(), any()))
                .thenReturn("https://github.com/org/repo/pull/1");

        AgentRequest request = new AgentRequest("Add USPS carrier adapter", Map.of(), "test");
        AgentResult result = agent.run(request);

        assertTrue(result.success());
        assertEquals("CarrierAdapterAgent", result.agentName());
        assertEquals("https://github.com/org/repo/pull/1", result.metadata().get("prUrl"));

        verify(githubService).createBranch(eq("feature/agent-usps-adapter"), eq("main-sha"));
        verify(githubService).pushFileToGitHub(
                eq("feature/agent-usps-adapter"),
                eq("src/main/java/io/github/hsummerhays/atlas/domain/model/Carrier.java"),
                any(), any(), eq("carrier-sha"));
        verify(githubService, times(2)).pushFileToGitHub(any(), any(), any(), any());
    }

    @Test
    void run_claudeResponseMissingCarrierLine_returnsFailureWithoutTouchingGitHub() {
        when(claudeService.analyze(any(), any())).thenReturn("not a structured response");

        AgentRequest request = new AgentRequest("Add USPS carrier adapter", Map.of(), "test");
        AgentResult result = agent.run(request);

        assertFalse(result.success());
        verifyNoInteractions(githubService);
    }

    @Test
    void run_carrierAlreadyRegistered_skipsEnumPatch() {
        String claudeResponse = """
                CARRIER: DHL
                ===FILE: src/main/java/io/github/hsummerhays/atlas/adapter/dhl/DhlExtra.java===
                package io.github.hsummerhays.atlas.adapter.dhl;
                public class DhlExtra {}
                """;
        when(claudeService.analyze(any(), any())).thenReturn(claudeResponse);
        when(githubService.getMainBranchSha()).thenReturn("main-sha");
        when(githubService.createBranch(any(), any())).thenReturn(true);
        when(githubService.getFileEntry(any())).thenReturn(new GitHubIntegrationService.FileEntry(
                "public enum Carrier {\n    FEDEX,\n    UPS,\n    DHL\n}\n", "carrier-sha"));
        when(githubService.pushFileToGitHub(any(), any(), any(), any())).thenReturn(true);
        when(githubService.createPullRequest(any(), any(), any(), any(), any()))
                .thenReturn("https://github.com/org/repo/pull/2");

        AgentRequest request = new AgentRequest("Extend DHL adapter", Map.of(), "test");
        AgentResult result = agent.run(request);

        assertTrue(result.success());
        verify(githubService, never()).pushFileToGitHub(any(),
                eq("src/main/java/io/github/hsummerhays/atlas/domain/model/Carrier.java"), any(), any(), any());
    }
}
