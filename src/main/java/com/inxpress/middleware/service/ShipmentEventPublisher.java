package com.inxpress.middleware.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inxpress.middleware.domain.model.Shipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Service
public class ShipmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventPublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String snsTopicArn;

    public ShipmentEventPublisher(
            SnsClient snsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sns.topic-arn:arn:aws:sns:us-east-1:123456789012:shipment-events}") String snsTopicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.snsTopicArn = snsTopicArn;
    }

    public void publishShipmentEvent(String eventType, Shipment shipment) {
        try {
            Map<String, Object> eventPayload = Map.of(
                    "eventType", eventType,
                    "trackingNumber", shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "N/A",
                    "carrier", shipment.getCarrier().name(),
                    "status", shipment.getStatus().name(),
                    "totalCost", shipment.getTotalCost() != null ? shipment.getTotalCost().toString() : "0.00",
                    "timestamp", System.currentTimeMillis()
            );

            String messageJson = objectMapper.writeValueAsString(eventPayload);
            log.info("Publishing event {} to SNS Topic {}: {}", eventType, snsTopicArn, messageJson);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(messageJson)
                    .build();

            snsClient.publish(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize shipment event payload", e);
        } catch (Exception e) {
            log.warn("Failed to publish event to AWS SNS (Verify AWS credentials/topics): {}", e.getMessage());
        }
    }
}
