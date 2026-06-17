package com.inxpress.middleware.adapter.usps;

import com.inxpress.middleware.adapter.CarrierAdapter;
import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.Shipment;
import com.inxpress.middleware.domain.model.ShipmentStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class UspsAdapter implements CarrierAdapter {

    private final UspsProperties properties;
    private final UspsTokenService tokenService;

    public UspsAdapter(UspsProperties properties, UspsTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    @Override
    public Carrier getCarrier() { return Carrier.USPS; }

    @Override
    public Shipment bookShipment(Shipment shipment) {
        shipment.setTrackingNumber("94001" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
        shipment.setStatus(ShipmentStatus.BOOKED);
        shipment.setTotalCost(quoteRate(shipment));
        return shipment;
    }

    @Override
    public BigDecimal quoteRate(Shipment shipment) {
        double totalWeight = shipment.getPackages().stream().mapToDouble(p -> p.weightInKg()).sum();
        return BigDecimal.valueOf(8.20 + (totalWeight * 1.50));
    }

    @Override
    public ShipmentStatus trackShipment(String trackingNumber) { return ShipmentStatus.IN_TRANSIT; }

    @Override
    public boolean cancelShipment(String trackingNumber) { return true; }
}
