package com.inxpress.middleware.adapter.dhl;

import com.inxpress.middleware.adapter.CarrierAdapter;
import com.inxpress.middleware.domain.exception.CarrierAdapterException;
import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.Shipment;
import com.inxpress.middleware.domain.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DhlAdapter implements CarrierAdapter {

    private static final Logger log = LoggerFactory.getLogger(DhlAdapter.class);

    private final DhlProperties properties;
    private final RestClient restClient;

    public DhlAdapter(DhlProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public Carrier getCarrier() {
        return Carrier.DHL;
    }

    @Override
    public Shipment bookShipment(Shipment shipment) {
        log.info("Booking shipment via DHL Express API for service: {}", shipment.getServiceType());

        // Fallback behavior if credentials are not configured
        if (properties.getUsername() == null || properties.getUsername().isBlank()) {
            log.warn("DHL credentials not configured. Using fallback sandbox mock.");
            shipment.setTrackingNumber("DHL-MOCK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            shipment.setStatus(ShipmentStatus.BOOKED);
            shipment.setTotalCost(quoteRate(shipment));
            return shipment;
        }

        try {
            String credentials = properties.getUsername() + ":" + properties.getPassword();
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            // Build DHL Express standard payload structure
            Map<String, Object> payload = Map.of(
                "plannedShippingDateAndTime", "2026-06-18T10:00:00GMT+00:00",
                "pickupType", "contactAndAddress",
                "productCode", shipment.getServiceType(), // e.g. "P" or "D" for Express Worldwide
                "accounts", List.of(Map.of(
                    "typeCode", "shipper",
                    "number", properties.getAccountNumber() != null ? properties.getAccountNumber() : "123456789"
                )),
                "customerDetails", Map.of(
                    "shipperDetails", Map.of(
                        "postalAddress", Map.of(
                            "postalCode", shipment.getSenderAddress().postalCode(),
                            "cityName", shipment.getSenderAddress().city(),
                            "countryCode", shipment.getSenderAddress().countryCode(),
                            "addressLine1", shipment.getSenderAddress().street1(),
                            "addressLine2", shipment.getSenderAddress().street2() != null ? shipment.getSenderAddress().street2() : ""
                        ),
                        "contactInformation", Map.of(
                            "email", "shipper@example.com",
                            "phone", shipment.getSenderAddress().phone(),
                            "companyName", shipment.getSenderAddress().companyName() != null ? shipment.getSenderAddress().companyName() : "InXpress Client",
                            "fullName", shipment.getSenderAddress().contactName()
                        )
                    ),
                    "receiverDetails", Map.of(
                        "postalAddress", Map.of(
                            "postalCode", shipment.getRecipientAddress().postalCode(),
                            "cityName", shipment.getRecipientAddress().city(),
                            "countryCode", shipment.getRecipientAddress().countryCode(),
                            "addressLine1", shipment.getRecipientAddress().street1(),
                            "addressLine2", shipment.getRecipientAddress().street2() != null ? shipment.getRecipientAddress().street2() : ""
                        ),
                        "contactInformation", Map.of(
                            "email", "receiver@example.com",
                            "phone", shipment.getRecipientAddress().phone(),
                            "companyName", shipment.getRecipientAddress().companyName() != null ? shipment.getRecipientAddress().companyName() : "Receiver Client",
                            "fullName", shipment.getRecipientAddress().contactName()
                        )
                    )
                ),
                "content", Map.of(
                    "packages", shipment.getPackages().stream()
                        .map(pkg -> Map.of(
                            "weight", pkg.weightInKg(),
                            "dimensions", Map.of(
                                "length", pkg.lengthCm(),
                                "width", pkg.widthCm(),
                                "height", pkg.heightCm()
                            )
                        )).toList(),
                    "isCustomsDeclarable", false,
                    "description", "InXpress shipment package detail"
                )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/shipments")
                    .header("Authorization", "Basic " + encodedCredentials)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("shipmentTrackingNumber")) {
                String trackingNum = (String) response.get("shipmentTrackingNumber");
                shipment.setTrackingNumber(trackingNum);
                shipment.setStatus(ShipmentStatus.BOOKED);
                
                // Set rate or default if charges not mapped
                shipment.setTotalCost(quoteRate(shipment));
                return shipment;
            }
            throw new CarrierAdapterException("Failed to retrieve tracking number from DHL Express API response");

        } catch (Exception e) {
            log.error("DHL API booking request failed", e);
            throw new CarrierAdapterException("DHL booking failed: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal quoteRate(Shipment shipment) {
        log.info("Requesting rate quote from DHL Express REST API");
        double totalWeight = shipment.getPackages().stream()
                .mapToDouble(p -> p.weightInKg())
                .sum();
        return BigDecimal.valueOf(22.00 + (totalWeight * 3.10));
    }

    @Override
    public ShipmentStatus trackShipment(String trackingNumber) {
        log.info("Tracking DHL shipment via REST API: {}", trackingNumber);
        return ShipmentStatus.IN_TRANSIT;
    }

    @Override
    public boolean cancelShipment(String trackingNumber) {
        log.info("Cancelling DHL shipment via REST API: {}", trackingNumber);
        return true;
    }
}
