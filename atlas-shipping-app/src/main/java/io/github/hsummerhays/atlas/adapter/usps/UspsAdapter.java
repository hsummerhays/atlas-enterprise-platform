package io.github.hsummerhays.atlas.adapter.usps;

import io.github.hsummerhays.atlas.adapter.AbstractCarrierAdapter;
import io.github.hsummerhays.atlas.adapter.auth.AuthenticationFactory;
import io.github.hsummerhays.atlas.adapter.auth.CarrierAuthenticator;
import io.github.hsummerhays.atlas.adapter.auth.CarrierConfiguration;
import io.github.hsummerhays.atlas.domain.model.Carrier;
import io.github.hsummerhays.atlas.domain.model.Shipment;
import io.github.hsummerhays.atlas.domain.model.ShipmentStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class UspsAdapter extends AbstractCarrierAdapter {

    private final UspsProperties properties;
    private final CarrierAuthenticator authenticator;

    public UspsAdapter(UspsProperties properties, UspsTokenService tokenService) {
        this.properties = properties;
        this.authenticator = AuthenticationFactory.createAuthenticator(
                CarrierConfiguration.oauth(properties.getBaseUrl(), "mock-secret"), // USPS simulated credentials
                tokenService::getAccessToken
        );
    }

    @Override
    public Carrier getCarrier() { return Carrier.USPS; }

    @Override
    public Shipment bookShipment(Shipment shipment) {
        String trackingNumber = "94001" + UUID.randomUUID().toString().substring(0, 15).toUpperCase();
        return mockBooking(shipment, "not yet integrated with a live carrier API", trackingNumber);
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
