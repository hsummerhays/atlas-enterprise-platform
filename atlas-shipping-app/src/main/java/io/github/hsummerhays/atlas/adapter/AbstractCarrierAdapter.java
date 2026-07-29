package io.github.hsummerhays.atlas.adapter;

import io.github.hsummerhays.atlas.domain.model.Shipment;
import io.github.hsummerhays.atlas.domain.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Shared sandbox-fallback behavior for CarrierAdapter implementations. Real carrier integrations
 * fall back to a fabricated booking (rather than failing) when the carrier isn't configured, so
 * that the shipment flow stays exercisable in dev/test environments without live credentials.
 */
public abstract class AbstractCarrierAdapter implements CarrierAdapter {

    private static final Logger log = LoggerFactory.getLogger(AbstractCarrierAdapter.class);

    protected static String randomMockTrackingNumber(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    protected final Shipment mockBooking(Shipment shipment, String reason, String trackingNumber) {
        log.warn("{}: {}. Using fallback sandbox mock.", getCarrier(), reason);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setStatus(ShipmentStatus.BOOKED);
        shipment.setTotalCost(quoteRate(shipment));
        return shipment;
    }
}
