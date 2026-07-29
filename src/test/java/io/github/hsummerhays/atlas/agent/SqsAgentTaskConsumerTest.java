package io.github.hsummerhays.atlas.agent;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SqsAgentTaskConsumerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private AgentCoordinator coordinator;

    private SqsAgentTaskConsumer consumer;

    private static final String QUEUE_URL = "http://localhost:4566/queue/agent-tasks";
    private static final String DLQ_URL = "http://localhost:4566/queue/agent-tasks-dlq";

    @BeforeEach
    void setUp() {
        consumer = new SqsAgentTaskConsumer(sqsClient, coordinator, new ObjectMapper());
        ReflectionTestUtils.setField(consumer, "queueUrl", QUEUE_URL);
        ReflectionTestUtils.setField(consumer, "dlqUrl", DLQ_URL);
        ReflectionTestUtils.setField(consumer, "maxReceiveCount", 3);
        // Coordinator mock defaults active count and threshold to 0, which would make the
        // load-shedding check (active >= threshold) always trip and skip polling entirely.
        when(coordinator.getThreshold()).thenReturn(5);
    }

    @Test
    void pollAgentTaskQueue_blankQueueUrl_skipsPolling() {
        ReflectionTestUtils.setField(consumer, "queueUrl", "");

        consumer.pollAgentTaskQueue();

        verifyNoInteractions(sqsClient);
    }

    @Test
    void pollAgentTaskQueue_normalMessage_processesAndDeletes() {
        Message message = mockMessage("msg-1", "receipt-1", validBody("documentationagent"), 1);
        stubReceive(message);
        when(coordinator.runAgent(any(), any())).thenReturn(
                new AgentResult("documentationagent", true, "done", Map.of()));

        consumer.pollAgentTaskQueue();

        verify(coordinator).runAgent(eq("documentationagent"), any());
        verify(sqsClient).deleteMessage(argThat(
                (DeleteMessageRequest req) -> "receipt-1".equals(req.receiptHandle())));
    }

    @Test
    void pollAgentTaskQueue_processingFails_doesNotDelete() {
        Message message = mockMessage("msg-2", "receipt-2", validBody("documentationagent"), 1);
        stubReceive(message);
        when(coordinator.runAgent(any(), any())).thenThrow(new RuntimeException("agent error"));

        consumer.pollAgentTaskQueue();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollAgentTaskQueue_poisonMessage_forwardsToDlqAndDeletes() {
        Message message = mockMessage("msg-3", "receipt-3", validBody("documentationagent"), 3);
        stubReceive(message);

        consumer.pollAgentTaskQueue();

        verify(coordinator, never()).runAgent(any(), any());

        ArgumentCaptor<SendMessageRequest> sendCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(sendCaptor.capture());
        assertThat(sendCaptor.getValue().queueUrl()).isEqualTo(DLQ_URL);

        verify(sqsClient).deleteMessage(argThat(
                (DeleteMessageRequest req) -> "receipt-3".equals(req.receiptHandle())));
    }

    @Test
    void pollAgentTaskQueue_poisonMessage_noDlqConfigured_justDeletes() {
        ReflectionTestUtils.setField(consumer, "dlqUrl", "");
        Message message = mockMessage("msg-4", "receipt-4", validBody("documentationagent"), 5);
        stubReceive(message);

        consumer.pollAgentTaskQueue();

        verify(coordinator, never()).runAgent(any(), any());
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
        verify(sqsClient).deleteMessage(argThat(
                (DeleteMessageRequest req) -> "receipt-4".equals(req.receiptHandle())));
    }

    private Message mockMessage(String id, String receipt, String body, int receiveCount) {
        Message message = mock(Message.class);
        when(message.messageId()).thenReturn(id);
        when(message.receiptHandle()).thenReturn(receipt);
        when(message.body()).thenReturn(body);
        when(message.attributes()).thenReturn(
                Map.of(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, String.valueOf(receiveCount)));
        return message;
    }

    private void stubReceive(Message... messages) {
        ReceiveMessageResponse response = mock(ReceiveMessageResponse.class);
        when(response.messages()).thenReturn(List.of(messages));
        when(sqsClient.receiveMessage(any(software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest.class))).thenReturn(response);
    }

    private String validBody(String agentName) {
        return "{\"agentName\":\"" + agentName + "\",\"inputData\":\"test input\",\"requestedBy\":\"test\"}";
    }
}
