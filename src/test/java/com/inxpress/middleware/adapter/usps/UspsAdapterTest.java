package com.inxpress.middleware.adapter.usps;

import com.inxpress.middleware.domain.model.Carrier;
import com.inxpress.middleware.domain.model.Shipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UspsAdapterTest {

    @Autowired
    private UspsAdapter uspsAdapter;

    @Test
    public void testUspsCarrierCode() {
        assertEquals(Carrier.USPS, uspsAdapter.getCarrier());
    }
}
