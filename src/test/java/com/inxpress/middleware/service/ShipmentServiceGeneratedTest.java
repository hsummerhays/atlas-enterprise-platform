package com.inxpress.middleware.service;

import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.Shipment;
import com.inxpress.middleware.domain.model.ShipmentStatus;
import com.inxpress.middleware.domain.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ShipmentServiceGeneratedTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    public void testCreateShipment() {
        Shipment shipment = new Shipment();
        shipment.setCarrier(Carrier.FEDEX);
        shipment.setServiceType("PRIORITY_OVERNIGHT");

        Shipment saved = shipmentService.createShipment(shipment);
        assertNotNull(saved.getId());
        assertEquals(ShipmentStatus.CREATED, saved.getStatus());
    }
}
