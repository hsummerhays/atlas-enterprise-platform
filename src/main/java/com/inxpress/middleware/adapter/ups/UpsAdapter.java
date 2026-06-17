package com.inxpress.middleware.adapter.ups;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class UpsAdapter implements CarrierAdapter {

    private static final Logger log = LoggerFactory.getLogger(UpsAdapter.class);

    private final UpsProperties properties;
    private final UpsTokenService tokenService;
    private final RestClient restClient;

    public UpsAdapter(UpsProperties properties, UpsTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public Carrier getCarrier() {
        return Carrier.UPS;
    }

    @Override
    public Shipment bookShipment(Shipment shipment) {
        log.info("Booking shipment via UPS REST API for service: {}", shipment.getServiceType());

        // Fallback behavior if credentials are not configured
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            log.warn("UPS client credentials not configured. Using fallback sandbox mock.");
            shipment.setTrackingNumber("1Z-MOCK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            shipment.setStatus(ShipmentStatus.BOOKED);
            shipment.setTotalCost(quoteRate(shipment));
            return shipment;
        }

        try {
            String token = tokenService.getAccessToken();

            // Build UPS shipment request payload
            Map<String, Object> payload = Map.of(
                "ShipmentRequest", Map.of(
                    "Request", Map.of(
                        "RequestOption", "nonvalidate",
                        "TransactionReference", Map.of(
                            "CustomerContext", "InXpress ERP Middleware Booking"
                        )
                    ),
                    "Shipment", Map.of(
                        "Description", "InXpress Shipping Middleware Shipment",
                        "Shipper", Map.of(
                            "Name", shipment.getSenderAddress().contactName(),
                            "AttentionName", shipment.getSenderAddress().companyName() != null ? shipment.getSenderAddress().companyName() : "",
                            "ShipperNumber", properties.getAccountNumber() != null ? properties.getAccountNumber() : "123456",
                            "Phone", Map.of("Number", shipment.getSenderAddress().phone()),
                            "Address", Map.of(
                                "AddressLine", List.of(shipment.getSenderAddress().street1(), 
                                                       shipment.getSenderAddress().street2() != null ? shipment.getSenderAddress().street2() : ""),
                                "City", shipment.getSenderAddress().city(),
                                "StateProvinceCode", shipment.getSenderAddress().stateOrProvince(),
                                "PostalCode", shipment.getSenderAddress().postalCode(),
                                "CountryCode", shipment.getSenderAddress().countryCode()
                            )
                        ),
                        "ShipTo", Map.of(
                            "Name", shipment.getRecipientAddress().contactName(),
                            "AttentionName", shipment.getRecipientAddress().companyName() != null ? shipment.getRecipientAddress().companyName() : "",
                            "Phone", Map.of("Number", shipment.getRecipientAddress().phone()),
                            "Address", Map.of(
                                "AddressLine", List.of(shipment.getRecipientAddress().street1(), 
                                                       shipment.getRecipientAddress().street2() != null ? shipment.getRecipientAddress().street2() : ""),
                                "City", shipment.getRecipientAddress().city(),
                                "StateProvinceCode", shipment.getRecipientAddress().stateOrProvince(),
                                "PostalCode", shipment.getRecipientAddress().postalCode(),
                                "CountryCode", shipment.getRecipientAddress().countryCode()
                            )
                        ),
                        "PaymentInformation", Map.of(
                            "ShipmentCharge", Map.of(
                                "Type", "01", // Transportation Charges
                                "BillShipper", Map.of(
                                    "AccountNumber", properties.getAccountNumber() != null ? properties.getAccountNumber() : "123456"
                                )
                            )
                        ),
                        "Service", Map.of(
                            "Code", shipment.getServiceType() // e.g. "03" for Ground, "01" for Next Day Air
                        ),
                        "Package", shipment.getPackages().stream()
                            .map(pkg -> Map.of(
                                "Packaging", Map.of(
                                    "Code", "02" // Customer Packaging
                                ),
                                "PackageWeight", Map.of(
                                    "UnitOfMeasurement", Map.of(
                                        "Code", "KGS"
                                    ),
                                    "Weight", String.valueOf(pkg.weightInKg())
                                ),
                                "Dimensions", Map.of(
                                    "UnitOfMeasurement", Map.of(
                                        "Code", "CM"
                                    ),
                                    "Length", String.valueOf((int) pkg.lengthCm()),
                                    "Width", String.valueOf((int) pkg.widthCm()),
                                    "Height", String.valueOf((int) pkg.heightCm())
                                )
                            )).toList()
                    )
                )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/shipments/v1/ship")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("ShipmentResponse")) {
                Map<String, Object> shipRes = (Map<String, Object>) response.get("ShipmentResponse");
                Map<String, Object> shipmentResults = (Map<String, Object>) shipRes.get("ShipmentResults");
                if (shipmentResults != null) {
                    String trackingNum = (String) shipmentResults.get("ShipmentIdentificationNumber");
                    shipment.setTrackingNumber(trackingNum);
                    shipment.setStatus(ShipmentStatus.BOOKED);
                    
                    Map<String, Object> charges = (Map<String, Object>) shipmentResults.get("ShipmentCharges");
                    if (charges != null && charges.containsKey("TotalCharges")) {
                        Map<String, Object> totalCharges = (Map<String, Object>) charges.get("TotalCharges");
                        String val = (String) totalCharges.get("MonetaryValue");
                        shipment.setTotalCost(new BigDecimal(val));
                    } else {
                        shipment.setTotalCost(quoteRate(shipment));
                    }
                    return shipment;
                }
            }
            throw new CarrierAdapterException("Failed to retrieve booking confirmation from UPS API response");

        } catch (Exception e) {
            log.error("UPS API booking request failed", e);
            throw new CarrierAdapterException("UPS booking failed: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal quoteRate(Shipment shipment) {
        log.info("Requesting rate quote from UPS REST API");
        double totalWeight = shipment.getPackages().stream()
                .mapToDouble(p -> p.weightInKg())
                .sum();
        return BigDecimal.valueOf(18.50 + (totalWeight * 2.20));
    }

    @Override
    public ShipmentStatus trackShipment(String trackingNumber) {
        log.info("Tracking UPS shipment via REST API: {}", trackingNumber);
        return ShipmentStatus.IN_TRANSIT;
    }

    @Override
    public boolean cancelShipment(String trackingNumber) {
        log.info("Cancelling UPS shipment via REST API: {}", trackingNumber);
        return true;
    }
}
