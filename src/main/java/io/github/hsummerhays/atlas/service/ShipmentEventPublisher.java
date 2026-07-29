package io.github.hsummerhays.atlas.service;

import io.github.hsummerhays.atlas.domain.model.Shipment;

/**
 * Port for publishing shipment lifecycle events. Keeps ShipmentService decoupled from the
 * concrete event transport (SNS today, potentially something else later).
 */
public interface ShipmentEventPublisher {
    void publishShipmentEvent(String eventType, Shipment shipment);
}
