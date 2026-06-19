package com.inxpress.middleware.service;

import com.inxpress.middleware.domain.model.Address;
import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.PackageDetail;
import com.inxpress.middleware.domain.model.Shipment;
import com.inxpress.middleware.domain.model.ShipmentStatus;
import com.inxpress.middleware.domain.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

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
        shipment.setSenderAddress(new Address(
                "Jane Sender", null, "123 Main St", null, "Austin", "TX", "78701", "US", "5125550100"));
        shipment.setRecipientAddress(new Address(
                "John Recipient", null, "456 Oak Ave", null, "Dallas", "TX", "75201", "US", "2145550199"));
        shipment.setPackages(List.of(new PackageDetail(2.5, 30, 20, 15, "Box", BigDecimal.TEN)));

        Shipment saved = shipmentService.createShipment(shipment);
        assertNotNull(saved.getId());
        assertEquals(ShipmentStatus.CREATED, saved.getStatus());
    }
}
