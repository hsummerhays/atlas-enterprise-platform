package io.github.hsummerhays.atlas.adapter.usps;

import io.github.hsummerhays.atlas.domain.model.Carrier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
