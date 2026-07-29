package io.github.hsummerhays.atlas.adapter;

import io.github.hsummerhays.atlas.domain.model.Carrier;
import io.github.hsummerhays.atlas.domain.model.Shipment;
import io.github.hsummerhays.atlas.domain.model.ShipmentStatus;

import java.math.BigDecimal;

public interface CarrierAdapter {
    
    Carrier getCarrier();
    
    Shipment bookShipment(Shipment shipment);
    
    BigDecimal quoteRate(Shipment shipment);
    
    ShipmentStatus trackShipment(String trackingNumber);
    
    boolean cancelShipment(String trackingNumber);
}
