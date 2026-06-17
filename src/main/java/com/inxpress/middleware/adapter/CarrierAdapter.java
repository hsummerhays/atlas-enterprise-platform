package com.inxpress.middleware.adapter;

import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.Shipment;
import com.inxpress.middleware.domain.model.ShipmentStatus;

import java.math.BigDecimal;

public interface CarrierAdapter {
    
    Carrier getCarrier();
    
    Shipment bookShipment(Shipment shipment);
    
    BigDecimal quoteRate(Shipment shipment);
    
    ShipmentStatus trackShipment(String trackingNumber);
    
    boolean cancelShipment(String trackingNumber);
}
